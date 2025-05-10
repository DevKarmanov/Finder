package karm.van.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.api.sync.RedisCommands;
import jakarta.servlet.http.HttpServletRequest;
import karm.van.config.properties.AdsMicroServiceProperties;
import karm.van.config.properties.AuthMicroServiceProperties;
import karm.van.config.properties.CommentMicroServiceProperties;
import karm.van.config.properties.ImageMicroServiceProperties;
import karm.van.dto.elastic.UserRequest;
import karm.van.dto.request.RecoveryRequest;
import karm.van.dto.request.UserDtoRequest;
import karm.van.dto.request.UserPatchRequest;
import karm.van.dto.response.*;
import karm.van.dto.rollBack.RollBackCommand;
import karm.van.exception.*;
import karm.van.model.AdminKey;
import karm.van.model.MyUser;
import karm.van.model.MyUserDocument;
import karm.van.repo.elasticRepo.ElasticRepo;
import karm.van.repo.jpaRepo.KeyRepo;
import karm.van.repo.jpaRepo.MyUserRepo;
import karm.van.service.utils.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class MyUserService {
    private final MyUserRepo userRepo;
    private final RedisCommands<String,String> redisCommands;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final AdsMicroServiceProperties adsProperties;
    private final ImageMicroServiceProperties imageProperties;
    private final CommentMicroServiceProperties commentProperties;
    private final AuthMicroServiceProperties authMicroServiceProperties;
    private final ApiService apiService;
    private final JwtService jwtService;
    private final NotificationProducer producer;
    private final PasswordEncoder encoder;
    private final KeyRepo keyRepo;
    private final ElasticRepo elasticRepo;

    @Value("${microservices.x-api-key}")
    private String apiKey;

    @Value("${server.port}")
    private String serverPort;

    public UserDtoResponse getUser(Authentication authentication, Optional<Long> userIdOpt) throws UsernameNotFoundException, BadCredentialsException {

        // Лямбда-функция для создания UserDtoResponse
        Function<MyUser, UserDtoResponse> mapToDto = user -> new UserDtoResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRoles(),
                user.getFirstName(),
                user.getLastName(),
                user.getDescription(),
                user.getCountry(),
                user.getRoleInCommand(),
                user.getSkills()
        );

        // Если передан userId, ищем по ID, иначе по username из токена
        return userIdOpt.map(userId ->
                        userRepo.findById(userId)
                                .map(mapToDto)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found with this ID")))
                .orElseGet(() ->
                        userRepo.findByName(authentication.getName())
                                .map(mapToDto)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found with this username")));
    }

    private List<UserCardResponse> sendRequestToGetUserCards(String token, Long userId) throws CardsNotGetedException {
        String uri = apiService.buildUrl(
                adsProperties.getPrefix(),
                adsProperties.getHost(),
                adsProperties.getPort(),
                adsProperties.getEndpoints().getGetUserCards(),
                userId
        );

        try {
            List<UserCardResponse> cards = apiService.getCardImagesRequest(uri,token,apiKey);
            if (cards==null){
                throw new CardsNotGetedException();
            }
            return cards;
        }catch (Exception e){
            throw new CardsNotGetedException("Due to an internal error, no results were received");
        }
    }

    private ProfileImageDtoResponse sendRequestToGetProfileImage(String token,Long imageId) throws ImageNotGetedException {
        String uri = apiService.buildUrl(
                imageProperties.getPrefix(),
                imageProperties.getHost(),
                imageProperties.getPort(),
                imageProperties.getEndpoints().getProfileImage(),
                imageId
        );

        try {
            ProfileImageDtoResponse profileImageDtoResponse = apiService.requestToGetProfileImage(uri,token,apiKey);
            if (profileImageDtoResponse==null){
                throw new ImageNotGetedException();
            }
            return profileImageDtoResponse;
        }catch (Exception e){
            throw new ImageNotGetedException("Due to an internal error, no results were received");
        }
    }

    private boolean redisKeyExist(String key){
        Long redisResult = redisCommands.exists(key);
        return redisResult!=null && redisResult>0;
    }

    public FullUserDtoResponse getFullUserData(Authentication authentication,HttpServletRequest request, String name) throws CardsNotGetedException, ImageNotGetedException, JsonProcessingException {
        MyUser user = userRepo.findByName(name)
                .orElseThrow(()->new UsernameNotFoundException("User with this name doesn't exist"));

        String redisKey = "user_"+user.getId();
        System.out.println("FULL DATA REDIS KEY: "+redisKey);
        if (redisKeyExist(redisKey)){
            return objectMapper.readValue(redisCommands.get(redisKey), FullUserDtoResponse.class);
        }else {
            return cacheUserInfo(authentication,request,user,redisKey);
        }
    }

    private FullUserDtoResponse cacheUserInfo(Authentication authentication,HttpServletRequest request, MyUser user, String redisKey) throws CardsNotGetedException, ImageNotGetedException, JsonProcessingException {

        String token = (String) request.getAttribute("jwtToken");

        try {
            List<UserCardResponse> cards = sendRequestToGetUserCards(token,user.getId());
            Long userProfileImage = user.getProfileImage();


            ProfileImageDtoResponse imageDtoResponse;
            if (userProfileImage>0){
                imageDtoResponse = sendRequestToGetProfileImage(token,user.getProfileImage());
            }else {
                imageDtoResponse = new ProfileImageDtoResponse(null,null);
            }

            FullUserDtoResponse fullUserDtoResponse = new FullUserDtoResponse(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRoles(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getDescription(),
                    user.getCountry(),
                    user.getRoleInCommand(),
                    user.getSkills(),
                    user.getFollowers().size(),
                    user.getFollowing().size(),
                    userRepo.getByName(authentication.getName()).getFollowing().contains(user.getId()),
                    user.isEnable(),
                    imageDtoResponse,
                    cards
            );

            String objectAsString = objectMapper.writeValueAsString(fullUserDtoResponse);
            redisCommands.set(redisKey,objectAsString);
            redisCommands.expire(redisKey,60);
            return fullUserDtoResponse;
        } catch (Exception e){
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void addCardToUser(Long userId,Long cardId) throws UsernameNotFoundException, BadCredentialsException{
        MyUser user = userRepo.findById(userId)
                .orElseThrow(()->new UsernameNotFoundException("User with this name doesn't exist"));
        List<Long> cardsList = user.getCards();
        cardsList.add(cardId);
        userRepo.save(user);
    }

    @Transactional
    public void registerUser(UserDtoRequest userDtoRequest) throws UserAlreadyExist {
        String adminKey = userDtoRequest.adminKey();
        AdminKey key = keyRepo.getReferenceById(1);
        List<String> userRoles = userDtoRequest.role();
        if(adminKey!=null && key.getAdminKey().toString().equals(adminKey.trim())){
            userRoles.add("ADMIN");
        }
        String name = userDtoRequest.name().trim();
        String email = userDtoRequest.email().trim();

        if (userRepo.existsByName(name)){
            throw new UserAlreadyExist("A user with this login already exists");
        }
        if(userRepo.existsByEmail(email)){
            throw new UserAlreadyExist("A user with this email already exists");
        }

        String country = userDtoRequest.country().trim();
        String description = userDtoRequest.description().trim();
        String firstname = userDtoRequest.firstName().trim();
        String lastname = userDtoRequest.lastName().trim();
        String roleInCommand = userDtoRequest.roleInCommand().trim();

        MyUser user = MyUser.builder()
                .name(name)
                .email(email)
                .country(country)
                .description(description)
                .roles(userRoles)
                .firstName(firstname)
                .lastName(lastname)
                .skills(userDtoRequest.skills())
                .password(passwordEncoder.encode(userDtoRequest.password().trim()))
                .roleInCommand(roleInCommand)
                .profileImage(0L)
                .unlockAt(LocalDateTime.now())
                .isEnable(true)
                .build();

        userRepo.save(user);

        saveInelastic(user.getId(),name,country,description,firstname,lastname,userDtoRequest.skills(),roleInCommand);
    }

    @Async
    protected void saveInelastic(Long id,String name,String country,String description,String firstname,String lastname,String skills,String roleInCommand){
        MyUserDocument userDocument = new MyUserDocument(id,name,country,description,firstname,lastname,skills,roleInCommand);
        producer.saveInElastic(userDocument);
    }

    private void sendRequestToDelUserCard(String token, Long cardId) throws UserNotDeletedException {
        try {
            HttpStatusCode httpStatusCode = apiService.requestToDelCard(
                    apiService.buildUrl(adsProperties.getPrefix(),
                            adsProperties.getHost(),
                            adsProperties.getPort(),
                            adsProperties.getEndpoints().getDelCard(),
                            cardId),token);
            if (httpStatusCode != HttpStatus.OK){
                throw new UserNotDeletedException("Due to an error on the server, your this user has not been deleted");
            }
        } catch (UserNotDeletedException e){
            throw e;
        } catch (Exception e){
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            throw new UserNotDeletedException("Due to an error on the server, your this user has not been deleted");
        }
    }

    private void moveProfileImageToTrashBucket(Long imageId) throws ImageNotMovedException {
        String imageUrl = apiService.buildUrl(
                imageProperties.getPrefix(),
                imageProperties.getHost(),
                imageProperties.getPort(),
                imageProperties.getEndpoints().getMoveProfileImage(),
                imageId
        );
        try {
            HttpStatusCode httpStatusCode = apiService.moveProfileImage(imageUrl, apiKey,true);
            if (httpStatusCode != HttpStatus.OK) {
                throw new ImageNotMovedException();
            }
        }catch (Exception e){
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            throw new ImageNotMovedException("An error occurred on the server side during the image moving");
        }
    }

    private List<CommentDto> requestToDeleteAllUserComments(Long userId, String token) throws CommentNotDeletedException {
        String commentUri = apiService.buildUrl(
                commentProperties.getPrefix(),
                commentProperties.getHost(),
                commentProperties.getPort(),
                commentProperties.getEndpoints().getDeleteCommentsByUser(),
                userId
        );
        try {
            return apiService.requestToDeleteAllUserComments(commentUri, token, apiKey);
        }catch (Exception e){
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            throw new CommentNotDeletedException("An error occurred while deleting user comments");
        }
    }

    @Async
    protected void rollBackComments(List<CommentDto> commentDtos) {
//        String url = apiService.buildUrl(
//                commentProperties.getPrefix(),
//                commentProperties.getHost(),
//                commentProperties.getPort(),
//                commentProperties.getEndpoints().getAddComments());
//
//        apiService.requestToRollBackDeletedComments(url,apiKey,commentDtos);

        try {
            String deletedCommentsJson = objectMapper.writeValueAsString(commentDtos);
            RollBackCommand rollBackCommand = RollBackCommand.builder()
                    .rollbackType("RestoreDeletedComments")
                    .params(Map.of("listOfComments",deletedCommentsJson))
                    .build();

            producer.sendRollBack(rollBackCommand);
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
    }


    private void deleteImageFromMinio(Long imageId,String token) throws ImageNotDeletedException {
        String url = apiService.buildUrl(
                imageProperties.getPrefix(),
                imageProperties.getHost(),
                imageProperties.getPort(),
                imageProperties.getEndpoints().getDelImageFromMinio(),
                imageId
        );

        try {
            HttpStatusCode httpStatusCode = apiService.deleteImageFromMinioRequest(url, token, apiKey);
            if (httpStatusCode != HttpStatus.OK) {
                throw new ImageNotDeletedException();
            }
        }catch (Exception e){
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            throw new ImageNotDeletedException("An error occurred on the server side during the image deleting");
        }
    }

    @Async
    protected void rollBackImages(Long imageId){
//        String imageUrl = apiService.buildUrl(
//                imageProperties.getPrefix(),
//                imageProperties.getHost(),
//                imageProperties.getPort(),
//                imageProperties.getEndpoints().getMoveProfileImage(),
//                imageId
//                );
//
//
//        apiService.moveProfileImage(imageUrl, apiKey,false);
        if (imageId>0){
            RollBackCommand rollBackCommand = RollBackCommand.builder()
                    .rollbackType("RestoreProfileImage")
                    .params(Map.of("imageId",imageId))
                    .build();
            producer.sendRollBack(rollBackCommand);
        }

    }

    private void deleteAllUserCards(MyUser user, String token) throws CardNotDeletedException {
        try {
            user.getCards().parallelStream()
                    .forEach(card-> {
                        try {
                            sendRequestToDelUserCard(token,card);
                        } catch (UserNotDeletedException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }catch (Exception e){
            throw new CardNotDeletedException();
        }
    }

    private void deleteAllComplaintByUserId(Long userId,String token) throws ComplaintsNotDeletedException {
        String uri = apiService.buildUrl(
                adsProperties.getPrefix(),
                adsProperties.getHost(),
                adsProperties.getPort(),
                adsProperties.getEndpoints().getDelAllComplaintByUserId(),
                userId
        );
        try {
            HttpStatusCode httpStatusCode = apiService.requestToDeleteAllComplaintByUserId(uri, token, apiKey);
            if (httpStatusCode != HttpStatus.OK) {
                throw new ImageNotMovedException();
            }
        }catch (Exception e){
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            throw new ComplaintsNotDeletedException("Due to an error on the server, the complaints were not deleted");
        }
    }

    @Transactional
    public void delUser(Authentication authentication, HttpServletRequest request) throws ImageNotMovedException, CommentNotDeletedException, ComplaintsNotDeletedException {
        String token = (String) request.getAttribute("jwtToken");
        MyUser user = userRepo.findByName(authentication.getName())
                .orElseThrow(()->new UsernameNotFoundException("User with this name doesn't exist"));

        String redisKey = "user_"+user.getId();

        Long userId = user.getId();

        List<CommentDto> comments = new ArrayList<>();
        try {
            Long userProfileImageId = user.getProfileImage();
            if (userProfileImageId>0){
                moveProfileImageToTrashBucket(userProfileImageId);
            }
            comments.addAll(requestToDeleteAllUserComments(userId,token));
            deleteAllComplaintByUserId(userId,token);
            if (!user.getCards().isEmpty()){
                deleteAllUserCards(user,token);
            }
            if (userProfileImageId>0){
                deleteImageFromMinio(userProfileImageId,token);
            }

            deleteRedisKeysByPatterns(user.getId(),user.getFollowing(),user.getFollowers());
            userRepo.delete(unsubscribeUser(user));
            delUserInElastic(user);
            redisCommands.del(redisKey);
        }catch (ImageNotMovedException e){
            log.error("Error moving the image");
            throw e;
        }catch (CommentNotDeletedException e){
            log.error("Error deleting comments");
            rollBackImages(user.getProfileImage());
            throw e;
        }catch (ComplaintsNotDeletedException e){
            log.error("Error deleting complaints");
            rollBackImages(user.getProfileImage());
            rollBackComments(comments);
            throw e;
        }catch (CardNotDeletedException e){
            log.error("Error deleting ads");
            rollBackImages(user.getProfileImage());
            rollBackComments(comments);
            //todo rollBackComplaints();
        }catch (ImageNotDeletedException e){
            log.error("Error deleting images");
            rollBackImages(user.getProfileImage());
            rollBackComments(comments);
            //todo rollBackComplaints();
            //todo rollBackCards();
        }
    }

    private MyUser unsubscribeUser(MyUser user){
        Long userId = user.getId();
        for (Long followingId : user.getFollowing()) {
            MyUser followingUser = userRepo.findById(followingId).orElseThrow(() -> new RuntimeException("Following user not found"));
            followingUser.getFollowers().remove(userId);
            userRepo.save(followingUser);
        }

        for (Long followerId : user.getFollowers()) {
            MyUser followerUser = userRepo.findById(followerId).orElseThrow(() -> new RuntimeException("Follower user not found"));
            followerUser.getFollowing().remove(userId);
            userRepo.save(followerUser);
        }

        user.setFollowers(new ArrayList<>());
        user.setFollowing(new ArrayList<>());

        return user;
    }

    @Async
    protected void delUserInElastic(MyUser user){
        producer.delInElastic(user);
    }

    @Transactional
    public void delUserCard(Authentication authentication, Long cardId){
        MyUser user = userRepo.findByName(authentication.getName())
                .orElseThrow(()->new UsernameNotFoundException("User with this name doesn't exist"));
        List<Long> userCards = user.getCards();
        userCards.remove(cardId);
        userRepo.save(user);
    }

    public boolean checkApiKeyNotEquals(String key) {
        return !apiKey.equals(key);
    }

    @Transactional
    public void addCommentToUser(Authentication authentication, Long commentId) {
        MyUser user = userRepo.findByName(authentication.getName())
                .orElseThrow(()->new UsernameNotFoundException("User with this name doesn't exist"));
        List<Long> commentsList = user.getComments();
        commentsList.add(commentId);
        userRepo.save(user);
    }

    @Transactional
    public void unlinkCommentAndUser(Long commentId, Long authorId){
        MyUser user = userRepo.getReferenceById(authorId);
        List<Long> commentsList = user.getComments();
        commentsList.remove(commentId);
        userRepo.save(user);
    }

    @Transactional
    public Long addProfileImage(Authentication authentication, Long profileImageId) {
        MyUser user = userRepo.findByName(authentication.getName())
                .orElseThrow(()->new UsernameNotFoundException("User with this name doesn't exist"));
        Long oldImageId = user.getProfileImage();
        user.setProfileImage(profileImageId);
        userRepo.save(user);

        return oldImageId;
    }


    @Transactional
    public void patchUser(Authentication authentication,
                          UserPatchRequest userPatchRequest) {

        MyUser user = userRepo.findByName(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User with this name doesn't exist"));

        String redisKey = "user_"+user.getId();

        System.out.println("PATCH REDIS KEY: "+redisKey);

        userPatchRequest.name().ifPresent(name -> {
            if (name.trim().isEmpty()) {
                throw new IllegalArgumentException("Name cannot be blank");
            }
            user.setName(name);
        });

        userPatchRequest.email().ifPresent(email -> {
            if (!userRepo.existsByEmail(email)) {
                user.setEmail(email);
            }
        });

        userPatchRequest.firstName().ifPresent(firstName -> {
            if (firstName.trim().isEmpty()) {
                throw new IllegalArgumentException("First name cannot be blank");
            }
            user.setFirstName(firstName);
        });

        userPatchRequest.lastName().ifPresent(lastName -> {
            if (lastName.trim().isEmpty()) {
                throw new IllegalArgumentException("Last name cannot be blank");
            }
            user.setLastName(lastName);
        });

        userPatchRequest.description().ifPresent(description -> {
            if (description.trim().isEmpty()) {
                throw new IllegalArgumentException("Description cannot be blank");
            }
            user.setDescription(description);
        });

        userPatchRequest.country().ifPresent(country -> {
            if (country.trim().isEmpty()) {
                throw new IllegalArgumentException("Country cannot be blank");
            }
            user.setCountry(country);
        });

        userPatchRequest.roleInCommand().ifPresent(roleInCommand -> {
            if (roleInCommand.trim().isEmpty()) {
                throw new IllegalArgumentException("Role in command cannot be blank");
            }
            user.setRoleInCommand(roleInCommand);
        });


        userPatchRequest.skills().ifPresent(skills -> {
            if (skills.length() > 255) {
                throw new IllegalArgumentException("Skills should not exceed 255 characters");
            }
            user.setSkills(skills);
        });

        userRepo.save(user);
        redisCommands.del(redisKey);
        patchInElastic(user.getId(),userPatchRequest);
    }

    @Async
    protected void patchInElastic(Long id,UserPatchRequest userPatchRequest){
        UserRequest userRequest = new UserRequest(
                id,
                userPatchRequest.name(),
                userPatchRequest.firstName(),
                userPatchRequest.lastName(),
                userPatchRequest.description(),
                userPatchRequest.country(),
                userPatchRequest.roleInCommand(),
                userPatchRequest.skills());
        System.out.println("ОТПРАВЛЕНО на PATCH");
        producer.patchInElastic(userRequest);
    }

    @Transactional
    public String toggleFavoriteCard(Authentication authentication, Long cardId) {
        String currentUserName = authentication.getName();

        MyUser user = userRepo.findByName(currentUserName)
                .orElseThrow(() -> new UsernameNotFoundException("User with this name doesn't exist"));

        String redisKey = "favorite-cards:"+currentUserName;

        List<Long> cards = user.getFavoriteCards();
        boolean cardRemove = false;

        if (cards.contains(cardId)){
            cards.remove(cardId);
            cardRemove = true;
        }else {
            cards.add(cardId);
        }

        if (redisKeyExist(redisKey)){
            redisCommands.del(redisKey);
        }

        userRepo.save(user);

        if (cardRemove){
            return "Card successfully deleted";
        }else {
            return "Card successfully added";
        }
    }

    public List<Long> getUserFavoriteCards(Authentication authentication) throws JsonProcessingException,UsernameNotFoundException {
        String currentUserName = authentication.getName();
        MyUser user = userRepo.findByName(currentUserName)
                .orElseThrow(() -> new UsernameNotFoundException("User with this name doesn't exist"));
        String redisKey = "favorite-cards:"+currentUserName;

        if (redisKeyExist(redisKey)){
            return objectMapper.readValue(redisCommands.get(redisKey), new TypeReference<>(){});
        }else {
            List<Long> favoriteCardsList = user.getFavoriteCards();
            String objectAsString = objectMapper.writeValueAsString(favoriteCardsList);
            redisCommands.set(redisKey,objectAsString);
            redisCommands.expire(redisKey,60);

            return favoriteCardsList;
        }
    }

    @Transactional
    public void blockUser(String userName,
                          int year,
                          int month,
                          int dayOfMonth,
                          int hours,
                          int minutes,
                          int seconds,
                          String reason) {

        MyUser user = userRepo.findByName(userName)
                .orElseThrow(() -> new UsernameNotFoundException("User with this name doesn't exist"));

        String redisKey = "user_"+user.getId();
        if (redisKeyExist(redisKey)){
            redisCommands.del(redisKey);
        }

        user.setEnable(false);
        user.setUnlockAt(LocalDateTime.of(year,month,dayOfMonth,hours,minutes,seconds));
        user.setBlockReason(reason);
        userRepo.save(user);
    }

    @Transactional
    public String toggleUserAuthorities(String userName) throws AccessDeniedException {
            MyUser user = userRepo.findByName(userName)
                    .orElseThrow(() -> new UsernameNotFoundException("User with this name doesn't exist"));

            List<String> roles = user.getRoles();
            boolean roleRemove = false;

            String redisKey = "user_"+user.getId();
            if (redisKeyExist(redisKey)){
                redisCommands.del(redisKey);
            }

            if (roles.contains("ADMIN")){
                roles.remove("ADMIN");
                roleRemove = true;
            }else {
                roles.add("ADMIN");
            }

            userRepo.save(user);

            if (roleRemove){
                return "User downgraded";
            }else {
                return "User promoted to admin";
            }
    }

    @Transactional
    public void unblockUser(String userName) {
        MyUser user = userRepo.findByName(userName)
                .orElseThrow(() -> new UsernameNotFoundException("User with this name doesn't exist"));

        String redisKey = "user_"+user.getId();
        if (redisKeyExist(redisKey)){
            redisCommands.del(redisKey);
        }

        user.setEnable(true);
        user.setUnlockAt(LocalDateTime.now());
        userRepo.save(user);
    }

    public void getRecoveryMail(RecoveryRequest request) {
        String url = apiService.buildUrl(
                authMicroServiceProperties.getPrefix(),
                authMicroServiceProperties.getHost(),
                serverPort,
                authMicroServiceProperties.getEndpoints().getRecoveryPassword(),
                jwtService.generateRecoveryToken(request.email(),request.password()));
        producer.sendRecoveryMessage(new RecoveryMessageDto(request.email(), url));
    }

    @Transactional
    public void updatePassword(String recoveryKey) throws EmailNotFoundException {
        if (jwtService.validateRecoveryToken(recoveryKey)){
            String password = jwtService.extractNewPassword(recoveryKey);
            String email = jwtService.extractUserEmail(recoveryKey);

            MyUser user = userRepo.findByEmail(email)
                    .orElseThrow(()->new EmailNotFoundException("Email doesn't exist"));

            user.setPassword(encoder.encode(password));
        }

    }

    @Transactional
    public UUID changeAdminKey() {
        AdminKey key = keyRepo.getReferenceById(1);
        String cacheKey = "adminKey:"+key;

        Long cacheResult = redisCommands.exists(cacheKey);
        if (cacheResult!=null && cacheResult>0){
            redisCommands.del(cacheKey);
        }

        UUID newKey = UUID.randomUUID();
        key.setAdminKey(newKey);
        keyRepo.save(key);

        redisCommands.set(cacheKey,newKey.toString());
        redisCommands.expire(cacheKey,60);
        return newKey;
    }

    public UUID getAdminKey() {
        UUID adminKey = keyRepo.getReferenceById(1).getAdminKey();
        String cacheKey = "adminKey:"+adminKey;

        Long cacheResult = redisCommands.exists(cacheKey);
        if (cacheResult!=null && cacheResult>0){
            return UUID.fromString(redisCommands.get(cacheKey));
        }else {
            redisCommands.set(cacheKey,adminKey.toString());
            redisCommands.expire(cacheKey,60);
            return adminKey;
        }
    }

    @Transactional
    public void unlinkFavoriteCardFromAllUsers(Long cardId) {
        List<MyUser> usersWithCard = userRepo.findAllByFavoriteCardsContaining(cardId);

        for (MyUser user : usersWithCard) {
            user.getFavoriteCards().remove(cardId);
            redisCommands.del("favorite-cards:"+user.getName());
        }

        userRepo.saveAll(usersWithCard);
    }


    public UserPageResponse searchUser(Optional<String> usernameOpt, Optional<String> countryOpt, Optional<String> descriptionOpt, Optional<String> firstnameOpt, Optional<String> lastnameOpt, Optional<String> skillsOpt, Optional<String> roleInCommandOpt, int pageNumber, int limit) throws SerializationException {

        StringBuilder redisKey = new StringBuilder("page:" + pageNumber + ":limit:" + limit);

        usernameOpt.ifPresent(username -> redisKey.append(":username:").append(username));
        countryOpt.ifPresent(country -> redisKey.append(":country:").append(country));
        descriptionOpt.ifPresent(description -> redisKey.append(":description:").append(description));
        firstnameOpt.ifPresent(firstname -> redisKey.append(":firstname:").append(firstname));
        lastnameOpt.ifPresent(lastname -> redisKey.append(":lastname:").append(lastname));
        skillsOpt.ifPresent(skills -> redisKey.append(":skills:").append(skills));
        roleInCommandOpt.ifPresent(role -> redisKey.append(":roleInCommand:").append(role));

        String redisFinalKey = redisKey.toString();

        Long redisResult = redisCommands.exists(redisFinalKey);
        if (redisResult != null && redisResult > 0) {
            try {
                return objectMapper.readValue(redisCommands.get(redisFinalKey), UserPageResponse.class);
            } catch (JsonProcessingException e) {
                throw new SerializationException("Error while deserializing from Redis");
            }
        }

        PageRequest pageRequest = PageRequest.of(pageNumber, limit);

        String username = usernameOpt.orElse("");
        String firstname = firstnameOpt.orElse("");
        String lastname = lastnameOpt.orElse("");
        String skills = skillsOpt.orElse("");
        String roleInCommand = roleInCommandOpt.orElse("");
        String description = descriptionOpt.orElse("");
        String country = countryOpt.orElse("");

        log.info("Searching users with parameters: username='{}', firstname='{}', lastname='{}', skills='{}', roleInCommand='{}', description='{}', country='{}', page={}, limit={}",
                username, firstname, lastname, skills, roleInCommand, description, country, pageNumber, limit);

        Page<MyUserDocument> page = elasticRepo.searchUsers(
                username, firstname, lastname, skills, roleInCommand, description, country, pageRequest
        );

        System.out.println("Result count: " + page.getTotalElements());

        UserPageResponse userPageResponse = new UserPageResponse(
                UserMapper.toDto(page.getContent()),
                page.isLast(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.isFirst(),
                page.getNumberOfElements()
        );

        cacheUsers(userPageResponse,redisFinalKey);
        return userPageResponse;
    }

    private <T> void cacheUsers(T objectToCash, String key) throws SerializationException {
        String objectAsString;

        try {
            objectAsString = objectMapper.writeValueAsString(objectToCash);
        } catch (JsonProcessingException e) {
            throw new SerializationException("an error occurred during deserialization");
        }

        redisCommands.set(key,objectAsString);
        redisCommands.expire(key,60);

    }

    @Transactional
    public String toggleFollowing(Authentication authentication, String userName) {
        MyUser author = userRepo.findByName(userName)
                .orElseThrow(() -> new UsernameNotFoundException("User with this id doesn't exist"));

        MyUser follower = userRepo.findByName(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User with this name doesn't exist"));

        if (author.getId().equals(follower.getId())){
            throw new IllegalArgumentException("You can't subscribe to yourself");
        }

        List<Long> authorFollowers = author.getFollowers();
        List<Long> followerFollowing = follower.getFollowing();

        String action;
        if (authorFollowers.contains(follower.getId())) {
            followerFollowing.remove(author.getId());
            authorFollowers.remove(follower.getId());
            action = "unsubscribe";
        } else {
            followerFollowing.add(author.getId());
            authorFollowers.add(follower.getId());
            action = "subscribe";
        }

        userRepo.save(author);
        userRepo.save(follower);

        String authorRedisKey = "user_" + author.getId();
        String followerRedisKey = "user_" + follower.getId();

        deleteRedisKeysByPatterns(author.getId());
        deleteRedisKeysByPatterns(follower.getId());

        redisCommands.del(authorRedisKey, followerRedisKey);

        return action;
    }

    private void deleteRedisKeysByPatterns(Long userId) {
        System.out.println("All Redis keys: " + redisCommands.keys("*"));

        List<String> followingKeys = redisCommands.keys("following:" + userId + ":pageNumber:*");
        List<String> followersKeys = redisCommands.keys("followers:" + userId + ":pageNumber:*");

        System.out.println("Followings: "+followingKeys);
        System.out.println("Followers: "+followersKeys);

        if (!followingKeys.isEmpty()) {
            redisCommands.del(followingKeys.toArray(new String[0]));
        }

        if (!followersKeys.isEmpty()) {
            redisCommands.del(followersKeys.toArray(new String[0]));
        }
    }

    @Async
    protected void deleteRedisKeysByPatterns(Long userId,List<Long> followingsList,List<Long> followersList) {
        if (!followingsList.isEmpty()){
            followingsList.forEach(id->{
                deleteRedisKeysByPatterns(id);
                redisCommands.del("user_"+id);
            });
        }

        if (!followersList.isEmpty()){
            followersList.forEach(id->{
                deleteRedisKeysByPatterns(id);
                redisCommands.del("user_"+id);
            });
        }

        deleteRedisKeysByPatterns(userId);

    }

    private <T> T checkCache(String key,Class<T> returnType) throws SerializationException {
        Long redisResult = redisCommands.exists(key);
        if (redisResult != null && redisResult > 0) {
            try {
                return objectMapper.readValue(redisCommands.get(key), returnType);
            } catch (JsonProcessingException e) {
                throw new SerializationException("Error while deserializing from Redis");
            }
        }else {
            return null;
        }
    }

    public SubscribersPageResponse getUserSubscriptions(String userName, int pageNumber, int limit, HttpServletRequest request) throws SerializationException, ImageNotGetedException {
        MyUser user = userRepo.findByName(userName)
                .orElseThrow(() -> new UsernameNotFoundException("User with this id doesn't exist"));

        String redisKey = "following:" + user.getId() + ":pageNumber:" + pageNumber + ":limit:" + limit;
        System.out.println("Saved to Redis with key: " + redisKey);
        SubscribersPageResponse cache = checkCache(redisKey, SubscribersPageResponse.class);
        if (cache != null) {
            return cache;
        }

        List<Long> userSubscriptions = user.getFollowing();
        if (userSubscriptions.isEmpty()) {
            return new SubscribersPageResponse(Collections.emptyList(), true, 0, 0, true, 0);
        }

        PageRequest pageRequest = PageRequest.of(pageNumber, limit);
        Page<Long> pageOfSubscriptions = new PageImpl<>(userSubscriptions, pageRequest, userSubscriptions.size());
        List<MyUser> subscribedUsers = userRepo.findAllById(pageOfSubscriptions.getContent());

        List<SubscriberDto> subscriberDtos = subscribedUsers.stream()
                .map(sub -> {
                    SubscriberDto dto = new SubscriberDto(
                            sub.getId(),
                            sub.getName(),
                            sub.getFirstName(),
                            sub.getLastName()
                    );
                    try {
                        dto.setProfileImage(getImageInfo(request, sub.getProfileImage()));
                    } catch (ImageNotGetedException e) {
                        throw new RuntimeException(e);
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        SubscribersPageResponse result = new SubscribersPageResponse(
                subscriberDtos,
                pageOfSubscriptions.isLast(),
                pageOfSubscriptions.getTotalPages(),
                pageOfSubscriptions.getTotalElements(),
                pageOfSubscriptions.isFirst(),
                pageOfSubscriptions.getNumberOfElements()
        );

        cacheUsers(result, redisKey);
        return result;
    }

    private ProfileImageDtoResponse getImageInfo(HttpServletRequest request,Long userProfileImage) throws ImageNotGetedException {
        String token = (String) request.getAttribute("jwtToken");
        if (userProfileImage>0){
            return sendRequestToGetProfileImage(token,userProfileImage);
        }else {
            return new ProfileImageDtoResponse(null,null);
        }
    }

    public SubscribersPageResponse getUserSubscribers(String userName, int pageNumber, int limit, HttpServletRequest request) throws SerializationException, ImageNotGetedException {
        MyUser user = userRepo.findByName(userName)
                .orElseThrow(() -> new UsernameNotFoundException("User with this id doesn't exist"));

        String redisKey = "followers:" + user.getId() + ":pageNumber:" + pageNumber + ":limit:" + limit;
        System.out.println("Saved to Redis with key: " + redisKey);
        SubscribersPageResponse cache = checkCache(redisKey, SubscribersPageResponse.class);
        if (cache != null) {
            return cache;
        }

        PageRequest pageRequest = PageRequest.of(pageNumber, limit);
        List<Long> userSubscribers = user.getFollowers();

        if (userSubscribers.isEmpty()) {
            return new SubscribersPageResponse(Collections.emptyList(), true, 0, 0, true, 0);
        }

        Page<Long> pageOfSubscribers = new PageImpl<>(userSubscribers, pageRequest, userSubscribers.size());
        List<MyUser> subscribedUsers = userRepo.findAllById(pageOfSubscribers.getContent());

        List<SubscriberDto> subscriberDtos = UserMapper.toSubscriberDto(subscribedUsers);

        for (SubscriberDto subscriberDto : subscriberDtos) {
            Long profileImageId = userRepo.getReferenceById(subscriberDto.getId()).getProfileImage();
            subscriberDto.setProfileImage(getImageInfo(request, profileImageId));
        }

        SubscribersPageResponse result = new SubscribersPageResponse(
                subscriberDtos,
                pageOfSubscribers.isLast(),
                pageOfSubscribers.getTotalPages(),
                pageOfSubscribers.getTotalElements(),
                pageOfSubscribers.isFirst(),
                pageOfSubscribers.getNumberOfElements()
        );

        cacheUsers(result, redisKey);
        return result;
    }

}
