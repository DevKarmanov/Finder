package karm.van.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.api.sync.RedisCommands;
import karm.van.config.properties.AuthenticationMicroServiceProperties;
import karm.van.config.properties.CommentMicroServiceProperties;
import karm.van.config.properties.ImageMicroServiceProperties;
import karm.van.dto.card.CardDto;
import karm.van.dto.card.CardPageResponseDto;
import karm.van.dto.card.ElasticPatchDto;
import karm.van.dto.card.FullCardDtoForOutput;
import karm.van.dto.comment.FullCommentDtoResponse;
import karm.van.dto.complaint.ComplaintType;
import karm.van.dto.image.ImageDto;
import karm.van.dto.message.EmailDataDto;
import karm.van.dto.rollBack.RollBackCommand;
import karm.van.dto.user.UserDtoRequest;
import karm.van.exception.card.CardNotDeletedException;
import karm.van.exception.card.CardNotFoundException;
import karm.van.exception.card.CardNotSavedException;
import karm.van.exception.card.CardNotUnlinkException;
import karm.van.exception.comment.CommentNotDeletedException;
import karm.van.exception.image.ImageLimitException;
import karm.van.exception.image.ImageNotDeletedException;
import karm.van.exception.image.ImageNotMovedException;
import karm.van.exception.image.ImageNotSavedException;
import karm.van.exception.other.SerializationException;
import karm.van.exception.other.TokenNotExistException;
import karm.van.exception.user.NotEnoughPermissionsException;
import karm.van.exception.user.UsernameNotFoundException;
import karm.van.model.CardDocument;
import karm.van.model.CardModel;
import karm.van.repo.jpaRepo.CardRepo;
import karm.van.repo.jpaRepo.ComplaintRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {
    private final CardRepo cardRepo;
    private final RedisCommands<String,String> redisCommands;
    private final ObjectMapper objectMapper;
    private final CommentMicroServiceProperties commentProperties;
    private final ImageMicroServiceProperties imageProperties;
    private final AuthenticationMicroServiceProperties authenticationProperties;
    private final ApiService apiService;
    private final ComplaintRepo complaintRepo;
    private final BrokerProducer brokerProducer;


    @Value("${microservices.x-api-key}")
    private String apiKey;

    @Value("${card.images.count}")
    private int allowedImagesCount;

    @Value("${email.settings.send}")
    private boolean send;

    void validateText(String title, String text) throws CardNotSavedException {
        if (title==null || text==null){
            throw new CardNotSavedException("The title and text should not be null");
        }else if (title.trim().isEmpty() || text.trim().isEmpty()){
            throw new CardNotSavedException("The title and text should not be empty");
        }
    }

    private CardModel addCardText(CardDto cardDto) throws CardNotSavedException {

        String title = cardDto.title();
        String text = cardDto.text();

        validateText(title,text);

        CardModel cardModel = CardModel.builder()
                .title(cardDto.title())
                .text(cardDto.text())
                .createTime(LocalDate.now())
                .build();

       return cardRepo.save(cardModel);
    }

    private void addCardText(CardDto cardDto, CardModel cardModel) throws CardNotSavedException {
        try {

            String title = cardDto.title();
            String text = cardDto.text();

            validateText(title,text);

            cardModel.setTitle(title);
            cardModel.setText(text);

            cardRepo.save(cardModel);
        }catch (CardNotSavedException e){
            log.debug("Card has not been saved: "+e.getMessage());
            throw new CardNotSavedException(e.getMessage());
        }
    }

    void checkToken(String token) throws TokenNotExistException {
        if (!apiService.validateToken(token,
                apiService.buildUrl(authenticationProperties.getPrefix(),
                        authenticationProperties.getHost(),
                        authenticationProperties.getPort(),
                        authenticationProperties.getEndpoints().getValidateToken()
                )
        )){
            throw new TokenNotExistException("Invalid token or expired");
        }
    }

    UserDtoRequest requestToGetUserByToken(String token) throws UsernameNotFoundException {
        UserDtoRequest user = apiService.getUserByToken(apiService.buildUrl(
                authenticationProperties.getPrefix(),
                authenticationProperties.getHost(),
                authenticationProperties.getPort(),
                authenticationProperties.getEndpoints().getUser()
        ), token,apiKey);

        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        return user;
    }

    UserDtoRequest requestToGetUserById(String token, Long userId) throws UsernameNotFoundException {
        UserDtoRequest user = apiService.getUserById(apiService.buildUrl(
                authenticationProperties.getPrefix(),
                authenticationProperties.getHost(),
                authenticationProperties.getPort(),
                authenticationProperties.getEndpoints().getUser()
        ), token,userId,apiKey);

        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        return user;
    }

    List<Long> requestToAddCardImages(List<MultipartFile> files, String token) throws ImageNotSavedException {
        String url = apiService.buildUrl(imageProperties.getPrefix(),
                imageProperties.getHost(),
                imageProperties.getPort(),
                imageProperties.getEndpoints().getAddCardImages());

        List<Long> imagesId = apiService.postRequestToAddCardImage(files, url, 0, token,apiKey);

        if (imagesId == null || imagesId.isEmpty()) {
            throw new ImageNotSavedException("Image IDs not returned");
        }

        return imagesId;
    }

    void requestToLinkCardAndUser(CardModel cardModel, Long authorId) throws CardNotSavedException {
        String url = apiService.buildUrl(
                authenticationProperties.getPrefix(),
                authenticationProperties.getHost(),
                authenticationProperties.getPort(),
                authenticationProperties.getEndpoints().getAddCardToUser(),
                cardModel.getId(),authorId);

        HttpStatusCode result;

        try {
            result = apiService.addCardToUser(url, apiKey);
        } catch (NullPointerException e) {
            log.error("Method addCardToUser returned the value null");
            throw e;
        }

        if (result != HttpStatus.OK) {
            log.error("The error occurred while the user was being assigned a card");
            throw new CardNotSavedException("An error occurred with saving");
        }
    }

    @Async
    protected void rollBackCard(Long cardId,Long userId) throws CardNotFoundException {
        CardModel cardModel = cardRepo.getCardModelById(cardId).orElseThrow(()->new CardNotFoundException("Card with this id doesn't found"));
        RollBackCommand rollBackCommand = RollBackCommand.builder()
                .rollbackType("CardAndUserLink")
                .params(Map.of("cardModel",cardModel,"userId",userId))
                .build();
        brokerProducer.sendRollBack(rollBackCommand);
        //requestToLinkCardAndUser(cardModel,token);
    }

    private void requestToDeleteImagesFromMinio(List<Long> imageIds){
        apiService.sendDeleteImagesFromMinioRequest(apiService.buildUrl(
                imageProperties.getPrefix(),
                imageProperties.getHost(),
                imageProperties.getPort(),
                imageProperties.getEndpoints().getDelImagesFromMinio()
        ), imageIds, apiKey);
    }

    @Async
    protected void asyncRollBackSavedImages(List<Long> imageIds){
        RollBackCommand rollBackCommand = RollBackCommand.builder()
                .rollbackType("DeleteSavedImages")
                .params(Map.of("listOfImageIds",imageIds))
                .build();
        brokerProducer.sendRollBack(rollBackCommand);
    }

    private void requestToUnlinkFavoriteCardFromAllUsers(Long cardId, String token) throws CardNotDeletedException {
        var url = apiService.buildUrl(
                authenticationProperties.getPrefix(),
                authenticationProperties.getHost(),
                authenticationProperties.getPort(),
                authenticationProperties.getEndpoints().getUnlinkFavoriteCardFromAllUsers(),cardId);
        System.out.println("URI: "+url);
        try {
            var status = apiService.requestToUnlinkFavoriteCardAndUser(url,token,apiKey);

            if (status.isError()){
                throw new CardNotDeletedException("An error occurred when deleting cards from favorites");
            }
        }catch (Exception e){
            throw new CardNotDeletedException(e.getMessage());
        }

    }

    @Async
    protected void addCardIntoElastic(CardModel cardModel){
        CardDocument cardDocument = CardDocument.builder()
                .id(cardModel.getId())
                .title(cardModel.getTitle())
                .text(cardModel.getText())
                .tags(cardModel.getTags())
                .createTime(cardModel.getCreateTime())
                .build();

        brokerProducer.saveInBroker(cardDocument);
    }

    @Async
    protected void sendMessage(EmailDataDto emailDataDto){
        brokerProducer.sendEmailMessage(emailDataDto);
    }

    @Transactional
    public void addCard(List<MultipartFile> files, CardDto cardDto, String authorization)
            throws ImageNotSavedException, CardNotSavedException, ImageLimitException, TokenNotExistException, UsernameNotFoundException {
        String token = authorization.substring(7);

        List<Long> imageIds = new ArrayList<>();
        try {
            checkToken(token);

            if (files.size() > allowedImagesCount) {
                throw new ImageLimitException("You have provided more than " + allowedImagesCount + " images");
            }

            UserDtoRequest user = requestToGetUserByToken(token);
            CardModel cardModel = addCardText(cardDto);
            imageIds = requestToAddCardImages(files,token);

            cardModel.setImgIds(imageIds);
            cardModel.setUserId(user.id());
            cardModel.setTags(cardDto.tags());
            cardRepo.save(cardModel);

            requestToLinkCardAndUser(cardModel,user.id());
            addCardIntoElastic(cardModel);
            invalidatePaginationCaches();
            if (send){
                sendMessage(new EmailDataDto(user.email(),cardDto));
            }

        } catch (ImageNotSavedException | ImageLimitException | UsernameNotFoundException | TokenNotExistException e) {
            log.debug("in class - " + e.getClass() + " an error has occurred: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            if (!imageIds.isEmpty()) {
                asyncRollBackSavedImages(imageIds);
            }
            log.error("class: " + e.getClass() + ", message: " + e.getMessage());
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }
    //todo сделать кеширование в отдельном потоке, когда уже информация найдена и отдана
    public FullCardDtoForOutput getCard(Long id, String authorization) throws CardNotFoundException, SerializationException, TokenNotExistException, UsernameNotFoundException {
        String token = authorization.substring(7);
        checkToken(token);
        String key = "card%d".formatted(id);
        Long redisResult = redisCommands.exists(key);
        if (redisResult==null || redisResult<=0) {//Если кеш отсутствует
            return cacheCard(id,key,token);
        } else {//Если кеш найден
            try {
                return objectMapper.readValue(redisCommands.get(key), FullCardDtoForOutput.class);//Десериализуем строку в объект и возвращаем
            } catch (JsonProcessingException e) {
                throw new SerializationException("an error occurred during serialization");
            }
        }
    }

    private List<ImageDto> requestToGetAllCardImages(CardModel card, String token){
        String url = apiService.buildUrl(
                imageProperties.getPrefix(),
                imageProperties.getHost(),
                imageProperties.getPort(),
                imageProperties.getEndpoints().getGetImages()
        );

        return apiService.getCardImagesRequest(card.getImgIds(),url,token,apiKey);
    }

    private FullCardDtoForOutput cacheCard(Long cardId, String key, String token) throws CardNotFoundException, SerializationException, UsernameNotFoundException {
        Optional<CardModel> cardModelOptional = cardRepo.getCardModelById(cardId);//Ищем запись в БД

        if (cardModelOptional.isEmpty()) {//Если записи в БД нет
            throw new CardNotFoundException("card with this id doesn't exist");//Ошибка о том, что карточки не существует
        }

        CardModel card = cardModelOptional.get();
        String objectAsString;

        List<ImageDto> images = requestToGetAllCardImages(card,token);

        String userName = requestToGetUserById(token,card.getUserId()).name();

        FullCardDtoForOutput fullCardDtoForOutput = new FullCardDtoForOutput(card.getId(),card.getTitle(),card.getText(),card.getCreateTime(),card.getTags(),images,userName);

        try {
            objectAsString = objectMapper.writeValueAsString(fullCardDtoForOutput);//Сериализуем объект в строку
        } catch (Exception e) {
            log.error("An error occurred during serialization for redis: "+e.getMessage());
            throw new SerializationException("an error occurred during serialization");
        }

        redisCommands.set(key, objectAsString);//Кешируем объект
        redisCommands.expire(key, 60);//Устанавливаем время жизни
        return fullCardDtoForOutput;
    }

    private void moveImagesToTrashBucket(List<Long> imagesId) throws ImageNotMovedException {
        String imageUrl = apiService.buildUrl(
                imageProperties.getPrefix(),
                imageProperties.getHost(),
                imageProperties.getPort(),
                imageProperties.getEndpoints().getMoveImage()
        );
        try {
            HttpStatusCode httpStatusCode = apiService.moveImagesToTrashPackage(imageUrl, imagesId, apiKey);
            if (httpStatusCode != HttpStatus.OK) {
                throw new ImageNotMovedException("An error occurred on the server side during the image moving");
            }
        }catch (Exception e){
            throw new ImageNotMovedException("An error occurred on the server side during the image moving");
        }
    }

    private List<FullCommentDtoResponse> sendRequestToDellAllComments(Long cardId, String token) throws CommentNotDeletedException {
        String commentUrl = apiService.buildUrl(
                commentProperties.getPrefix(),
                commentProperties.getHost(),
                commentProperties.getPort(),
                commentProperties.getEndpoints().getDellAllCommentsByCard(),
                cardId
        );
        try {
            return apiService.requestToDelAllCommentsByCard(commentUrl,token,apiKey);
        } catch (Exception e){
            throw new CommentNotDeletedException(e.getMessage());
        }
    }

    @Async
    protected void rollBackImages(List<Long> imagesId){
//        String imageUrl = apiService.buildUrl(
//                imageProperties.getPrefix(),
//                imageProperties.getHost(),
//                imageProperties.getPort(),
//                imageProperties.getEndpoints().getMoveImage());
//
//
//        apiService.moveImagesToImagePackage(imageUrl, imagesId, token, apiKey);
        RollBackCommand rollBackCommand = RollBackCommand.builder()
                .rollbackType("MoveImagesToImagePackage")
                .params(Map.of("listOfImageIds",imagesId))
                .build();
        brokerProducer.sendRollBack(rollBackCommand);
    }

    private void checkUserPermissions(String token, CardModel cardModel) throws UsernameNotFoundException, NotEnoughPermissionsException {

        UserDtoRequest user;

        try {
            user = requestToGetUserByToken(token);
        }catch (UsernameNotFoundException e) {
            throw new UsernameNotFoundException("User with this token doesn't exist");
        }

        Long userId = user.id();
        List<String> userRoles = user.role();

        if ( (!userId.equals(cardModel.getUserId())) && (userRoles.stream().noneMatch(role->role.equals("ADMIN"))) ){
            throw new NotEnoughPermissionsException("You don't have permission to do this");
        }
    }

    private void requestToUnlinkCardFromUser(String token, Long cardId) throws CardNotUnlinkException {
        HttpStatusCode httpStatusCode = apiService.requestToUnlinkCardFromUser(apiService.buildUrl(
                authenticationProperties.getPrefix(),
                authenticationProperties.getHost(),
                authenticationProperties.getPort(),
                authenticationProperties.getEndpoints().getUnlinkCardFromUser(),
                cardId
        ),token,apiKey);

        if (httpStatusCode != HttpStatus.OK){
            throw new CardNotUnlinkException("An error occurred while trying to delete the card");
        }
    }
    
    @Async
    protected void delCardIntoElastic(CardModel cardModel) {
        brokerProducer.saveInBroker(cardModel);
    }

    @Async
    protected void rollBackComments(List<FullCommentDtoResponse> deletedComments){
        try {
            String deletedCommentsJson = objectMapper.writeValueAsString(deletedComments);
            RollBackCommand rollBackCommand = RollBackCommand.builder()
                    .rollbackType("RestoreDeletedComments")
                    .params(Map.of("listOfComments",deletedCommentsJson))
                    .build();

            brokerProducer.sendRollBack(rollBackCommand);
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }

    }


    private void invalidatePaginationCaches() {
        String tag = "cards_pagination";
        Set<String> keys = redisCommands.smembers(tag);

        if (!keys.isEmpty()) {
            redisCommands.del(keys.toArray(new String[0]));
            redisCommands.del(tag);
        }
    }

    @Transactional
    public void deleteCard(Long cardId, String authorization) throws CardNotFoundException, TokenNotExistException, CommentNotDeletedException, ImageNotMovedException, UsernameNotFoundException, NotEnoughPermissionsException, CardNotSavedException, CardNotDeletedException {
        String token = authorization.substring(7);
        checkToken(token);

        String key = String.format("card%d", cardId);

        CardModel cardModel = cardRepo.getCardModelById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card with this id doesn't exist"));

        Long authorId = cardModel.getUserId();

        checkUserPermissions(token,cardModel);

        complaintRepo.deleteAllByTargetIdAndComplaintType(cardId, ComplaintType.CARD);

        redisCommands.del(key);

        List<Long> imagesId = cardModel.getImgIds();
        List<FullCommentDtoResponse> deletedComments = new ArrayList<>();
        try {
            requestToUnlinkCardFromUser(token,cardId);
            moveImagesToTrashBucket(imagesId);
            deletedComments.addAll(sendRequestToDellAllComments(cardId,token));
            requestToUnlinkFavoriteCardFromAllUsers(cardId,token);

            requestToDeleteImagesFromMinio(imagesId);

            cardRepo.deleteById(cardId);
            delCardIntoElastic(cardModel);
            invalidatePaginationCaches();
        } catch (ImageNotMovedException e) {
            rollBackCard(cardId,authorId);
            throw e;
        } catch (CommentNotDeletedException e){
            rollBackImages(imagesId);
            rollBackCard(cardId,authorId);
            throw e;
        } catch (CardNotDeletedException e){
            rollBackImages(imagesId);
            rollBackCard(cardId,authorId);
            rollBackComments(deletedComments);
            throw e;
        }catch (Exception e) {
            log.error("An unknown error occurred while deleting the card: "+e.getMessage()+" - "+e.getClass());
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }


    private List<FullCardDtoForOutput> getFullCardsDto(String token, Page<CardModel> page){
        return page.getContent().stream()
                .map(card -> {
                    try {
                        return new FullCardDtoForOutput(
                                card.getId(),
                                card.getTitle(),
                                card.getText(),
                                card.getCreateTime(),
                                card.getTags(),
                                requestToGetAllCardImages(card,token),
                                requestToGetUserById(token,card.getUserId()).name());
                    } catch (UsernameNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }).toList();
    }

    public CardPageResponseDto cachingAndCreateDto(Page<CardModel> page, String token, String redisKey) throws SerializationException {
        CardPageResponseDto cardPageResponseDto = new CardPageResponseDto(
                getFullCardsDto(token,page),
                page.isLast(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.isFirst(),
                page.getNumberOfElements());

        try {
            String objectAsString = objectMapper.writeValueAsString(cardPageResponseDto);
            redisCommands.set(redisKey,objectAsString);
            redisCommands.expire(redisKey,60);
        } catch (JsonProcessingException e) {
            throw new SerializationException("an error occurred during deserialization");
        }

        return cardPageResponseDto;
    }


    public CardPageResponseDto getAllCards(int pageNumber, int limit, String authorization) throws TokenNotExistException, SerializationException {
        String token = authorization.substring(7);
        checkToken(token);

        String key = "pageNumber:"+pageNumber+":limit:"+limit;
        Long redisResult = redisCommands.exists(key);
        if (redisResult!=null && redisResult>0){
            try {
                return objectMapper.readValue(redisCommands.get(key), CardPageResponseDto.class);
            } catch (JsonProcessingException e) {
                throw new SerializationException("an error occurred during serialization");
            }
        }else {
            Page<CardModel> page = cardRepo.findAll(PageRequest.of(pageNumber,limit));
            CardPageResponseDto response = cachingAndCreateDto(page, token, key);
            redisCommands.sadd("cards_pagination", key);  // Добавляем в тег ПОСЛЕ успешного кеширования
            return response;
        }

    }

    @Async
    protected void patchCardTextIntoElastic(Long id,CardDto cardDto){
        brokerProducer.saveInBroker(new ElasticPatchDto(id,cardDto));
    }

    @Transactional
    public void patchCard(Long id, Optional<CardDto> cardDtoOptional, Optional<List<MultipartFile>> optFiles, String authorization) throws CardNotFoundException, CardNotSavedException, ImageNotSavedException, ImageLimitException, TokenNotExistException, UsernameNotFoundException, NotEnoughPermissionsException {
        System.out.println("пришел запрос: "+cardDtoOptional);
        String token = authorization.substring(7);
        checkToken(token);
        CardModel cardModel = cardRepo.getCardModelById(id)
                .orElseThrow(() -> new CardNotFoundException("Card with this id doesn't exist"));

        checkUserPermissions(token,cardModel);

        String key = "card%d".formatted(id);
        boolean cardChange = false;

        if (cardDtoOptional.isPresent()) {
            try {
                CardDto cardDto = cardDtoOptional.get();
                addCardText(cardDto, cardModel);
                cardModel.setTags(cardDto.tags());
                patchCardTextIntoElastic(id,cardDto);
                cardChange = true;
            } catch (CardNotSavedException e) {
                throw new CardNotSavedException(e.getMessage());
            }
        }
        Long redisResult = redisCommands.exists(key);
        if (cardChange && (redisResult!=null && redisResult>0)){
            UserDtoRequest user = requestToGetUserByToken(token);
            String redisProfileKey = "user_"+user.id();
            redisCommands.del(key,redisProfileKey);
            invalidatePaginationCaches();
        }

        if (optFiles.isPresent()) {
            String url = apiService.buildUrl(imageProperties.getPrefix(),
                    imageProperties.getHost(),
                    imageProperties.getPort(),
                    imageProperties.getEndpoints().getAddCardImages());
            try {
                List<Long> imageIds = apiService.postRequestToAddCardImage(optFiles.get(), url, cardModel.getImgIds().size(),token,apiKey);

                if (imageIds == null || imageIds.isEmpty()) {
                    throw new ImageNotSavedException("An error occurred and the images were not saved");
                }else {
                    List<Long> currentImagesId = cardModel.getImgIds();
                    imageIds.parallelStream().forEach(currentImagesId::add);
                    cardModel.setImgIds(currentImagesId);
                    cardRepo.save(cardModel);
                }
            }catch (WebClientResponseException.BadRequest e){
                throw new ImageLimitException("There is a maximum number of images in this ad");
            }
        }
    }

    private HttpStatusCode sendRequestToDeleteInoImageFromDB(Long imageId) throws ImageNotDeletedException {
        try {
            return apiService.requestToDeleteOneImageFromDB(apiService.buildUrl(
                    imageProperties.getPrefix(),
                    imageProperties.getHost(),
                    imageProperties.getPort(),
                    imageProperties.getEndpoints().getDelOneImageFromCard(),
                    imageId
            ),apiKey);
        } catch (Exception e){
            throw new ImageNotDeletedException("Due to an error, the image was not deleted");
        }

    }

    @Transactional
    public void delOneImageInCard(Long cardId, Long imageId, String authorization) throws CardNotFoundException, TokenNotExistException, ImageNotDeletedException, UsernameNotFoundException, NotEnoughPermissionsException {
        String token = authorization.substring(7);
        checkToken(token);
        CardModel card = cardRepo.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card with id doesn't exist"));

        checkUserPermissions(token,card);

        if (sendRequestToDeleteInoImageFromDB(imageId) != HttpStatus.OK){
            throw new ImageNotDeletedException("Due to an error, the image was not deleted");
        }

        card.getImgIds().remove(imageId);

        String key = "card%d".formatted(cardId);

        Long redisResult = redisCommands.exists(key);
        if (redisResult!=null && redisResult>0){
            redisCommands.del(key);
        }
        
        cardRepo.save(card);
    }

    public Boolean checkApiKey(String apiKey){
        return apiKey.equals(this.apiKey);
    }

    public List<CardDto> getAllUserCards(String authorization, String apiKey, Long userId) throws TokenNotExistException {
        checkToken(authorization.substring(7));
        if (!checkApiKey(apiKey)){
            throw new TokenNotExistException("Invalid apiKey");
        }

        return cardRepo.findAllByUserId(userId)
                .parallelStream()
                .map(card->new CardDto(card.getId(), card.getTitle(),card.getText(),card.getTags()))
                .toList();

    }
}
