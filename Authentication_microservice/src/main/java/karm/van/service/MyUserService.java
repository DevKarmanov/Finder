package karm.van.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import karm.van.config.AdsMicroServiceProperties;
import karm.van.config.AuthMicroServiceProperties;
import karm.van.config.ImageMicroServiceProperties;
import karm.van.dto.request.RecoveryRequest;
import karm.van.dto.request.UserDtoRequest;
import karm.van.dto.request.UserPatchRequest;
import karm.van.dto.response.*;
import karm.van.exception.*;
import karm.van.model.MyUser;
import karm.van.repo.MyUserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Log4j2
public class MyUserService {
    private final MyUserRepo userRepo;
    private final Jedis redis;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final AdsMicroServiceProperties adsProperties;
    private final ImageMicroServiceProperties imageProperties;
    private final AuthMicroServiceProperties authMicroServiceProperties;
    private final ApiService apiService;
    private final JwtService jwtService;
    private final NotificationProducer notificationProducer;
    private final PasswordEncoder encoder;

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

    public FullUserDtoResponse getFullUserData(HttpServletRequest request, String name) throws CardsNotGetedException, ImageNotGetedException, JsonProcessingException {
        String redisKey = "user_"+name;
        if (redis.exists(redisKey)){
            return objectMapper.readValue(redis.get(redisKey), FullUserDtoResponse.class);
        }else {
            return cacheUserInfo(request,name,redisKey);
        }
    }

    private FullUserDtoResponse cacheUserInfo(HttpServletRequest request, String name, String redisKey) throws CardsNotGetedException, ImageNotGetedException, JsonProcessingException {
        MyUser user = userRepo.findByName(name)
                .orElseThrow(()->new UsernameNotFoundException("User with this name doesn't exist"));

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
                    imageDtoResponse,
                    cards
            );

            String objectAsString = objectMapper.writeValueAsString(fullUserDtoResponse);
            redis.set(redisKey,objectAsString);
            redis.expire(redisKey,60);
            return fullUserDtoResponse;
        } catch (Exception e){
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void addCardToUser(Authentication authentication,Long cardId) throws UsernameNotFoundException, BadCredentialsException{
        MyUser user = userRepo.findByName(authentication.getName())
                .orElseThrow(()->new UsernameNotFoundException("User with this name doesn't exist"));
        List<Long> cardsList = user.getCards();
        cardsList.add(cardId);
        userRepo.save(user);
    }

    @Transactional
    public void registerUser(UserDtoRequest userDtoRequest) throws UserAlreadyExist {
        String name = userDtoRequest.name();
        String email = userDtoRequest.email();

        if (userRepo.existsByName(name)){
            throw new UserAlreadyExist("A user with this login already exists");
        }
        if(userRepo.existsByEmail(email)){
            throw new UserAlreadyExist("A user with this email already exists");
        }

        MyUser user = MyUser.builder()
                .name(name)
                .email(email)
                .country(userDtoRequest.country())
                .description(userDtoRequest.description())
                .roles(userDtoRequest.role())
                .firstName(userDtoRequest.firstName())
                .lastName(userDtoRequest.lastName())
                .skills(userDtoRequest.skills())
                .password(passwordEncoder.encode(userDtoRequest.password()))
                .roleInCommand(userDtoRequest.roleInCommand())
                .profileImage(0L)
                .unlockAt(LocalDateTime.now())
                .isEnable(true)
                .build();

        userRepo.save(user);

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

    private void moveProfileImageToTrashBucket(Long imageId, String token) throws ImageNotMovedException {
        String imageUrl = apiService.buildUrl(
                imageProperties.getPrefix(),
                imageProperties.getHost(),
                imageProperties.getPort(),
                imageProperties.getEndpoints().getMoveProfileImage(),
                imageId
        );
        try {
            HttpStatusCode httpStatusCode = apiService.moveProfileImage(imageUrl, token, apiKey,true);
            if (httpStatusCode != HttpStatus.OK) {
                throw new ImageNotMovedException();
            }
        }catch (Exception e){
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            throw new ImageNotMovedException("An error occurred on the server side during the image moving");
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
    protected void rollBackImages(Long imageId, String token){
        String imageUrl = apiService.buildUrl(
                imageProperties.getPrefix(),
                imageProperties.getHost(),
                imageProperties.getPort(),
                imageProperties.getEndpoints().getMoveProfileImage(),
                imageId
                );


        apiService.moveProfileImage(imageUrl, token, apiKey,false);
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
    public void delUser(Authentication authentication, HttpServletRequest request) throws ImageNotMovedException, CardNotDeletedException, ImageNotDeletedException, ComplaintsNotDeletedException {
        String token = (String) request.getAttribute("jwtToken");
        String redisKey = "user_"+authentication.getName();
        MyUser user = userRepo.findByName(authentication.getName())
                .orElseThrow(()->new UsernameNotFoundException("User with this name doesn't exist"));
        try {
            Long userProfileImageId = user.getProfileImage();
            if (userProfileImageId>0){
                moveProfileImageToTrashBucket(userProfileImageId,token);
            }
            deleteAllComplaintByUserId(user.getId(),token);
            if (!user.getCards().isEmpty()){
                deleteAllUserCards(user,token);
            }
            if (userProfileImageId>0){
                deleteImageFromMinio(userProfileImageId,token);
            }
            userRepo.delete(user);
            redis.del(redisKey);
        }catch (ImageNotMovedException | ImageNotDeletedException e){
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            throw e;
        }catch (CardNotDeletedException |ComplaintsNotDeletedException e){
            rollBackImages(user.getProfileImage(),token);
            throw e;
        }
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

        String redisKey = "user_"+authentication.getName();


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
        redis.del(redisKey);
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

        if (redis.exists(redisKey)){
            redis.del(redisKey);
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

        if (redis.exists(redisKey)){
            return objectMapper.readValue(redis.get(redisKey), new TypeReference<>(){});
        }else {
            List<Long> favoriteCardsList = user.getFavoriteCards();
            String objectAsString = objectMapper.writeValueAsString(favoriteCardsList);
            redis.set(redisKey,objectAsString);
            redis.expire(redisKey,60);

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

            if (roles.contains("ROLE_ADMIN")){
                roles.remove("ROLE_ADMIN");
                roleRemove = true;
            }else {
                roles.add("ROLE_ADMIN");
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
        notificationProducer.sendRecoveryMessage(new RecoveryMessageDto(request.email(), url));
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
}
