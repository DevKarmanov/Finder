package karm.van.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.api.sync.RedisCommands;
import karm.van.config.AuthenticationMicroServiceProperties;
import karm.van.dto.CommentAuthorDto;
import karm.van.dto.CommentDto;
import karm.van.dto.CommentDtoResponse;
import karm.van.dto.UserDtoRequest;
import karm.van.exception.card.CardNotFoundException;
import karm.van.exception.comment.CommentNotFoundException;
import karm.van.exception.comment.CommentNotSavedException;
import karm.van.exception.comment.CommentNotUnlinkException;
import karm.van.exception.other.InvalidDataException;
import karm.van.exception.other.SerializationException;
import karm.van.exception.token.TokenNotExistException;
import karm.van.exception.user.NotEnoughPermissionsException;
import karm.van.exception.user.UsernameNotFoundException;
import karm.van.model.CardModel;
import karm.van.model.CommentModel;
import karm.van.repo.CardRepo;
import karm.van.repo.CommentRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log4j2
public class CommentService {
    private final CommentRepo commentRepo;
    private final CardRepo cardRepo;
    private final ObjectMapper objectMapper;
    private final AuthenticationMicroServiceProperties authProperties;
    private final ApiService apiService;

    @Value("${microservices.x-api-key}")
    private String apiKey;
    private final RedisCommands<String,String> redisCommands;

    private void checkToken(String token) throws TokenNotExistException {
        if (!apiService.validateToken(token,
                apiService.buildUrl(authProperties.getPrefix(),
                        authProperties.getHost(),
                        authProperties.getPort(),
                        authProperties.getEndpoints().getValidateToken()
                )
        )){
            throw new TokenNotExistException("Invalid token or expired");
        }
    }

    public boolean checkNoneEqualsApiKey(String key) {
        return !key.equals(apiKey);
    }

    private UserDtoRequest requestToGetUserByToken(String token) throws UsernameNotFoundException {
        UserDtoRequest user = apiService.getUserByToken(apiService.buildUrl(
                authProperties.getPrefix(),
                authProperties.getHost(),
                authProperties.getPort(),
                authProperties.getEndpoints().getUser()
        ),token,apiKey);

        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        return user;
    }

    private UserDtoRequest requestToGetUserById(String token, Long userId) throws UsernameNotFoundException {
        UserDtoRequest user = apiService.getUserById(apiService.buildUrl(
                authProperties.getPrefix(),
                authProperties.getHost(),
                authProperties.getPort(),
                authProperties.getEndpoints().getUser()
        ), token,userId,apiKey);

        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        return user;
    }

    private void requestToUnlinkCommentFromUser(String token, Long commentId, Long authorId) throws CommentNotUnlinkException {
        HttpStatusCode httpStatusCode = apiService.requestToUnlinkCommentFromUser(apiService.buildUrl(
                authProperties.getPrefix(),
                authProperties.getHost(),
                authProperties.getPort(),
                authProperties.getEndpoints().getUnlinkCommentAndUser(),
                commentId,
                authorId
        ),token,apiKey);

        if (httpStatusCode != HttpStatus.OK){
            throw new CommentNotUnlinkException("An error occurred while trying to delete the card");
        }
    }

    private void requestToLinkCommentAndUser(Long commentId, String token) throws CommentNotSavedException {
        String url = apiService.buildUrl(
                authProperties.getPrefix(),
                authProperties.getHost(),
                authProperties.getPort(),
                authProperties.getEndpoints().getLinkCommentAndUser(),
                commentId);

        try {
            if (apiService.addCommentToUser(url,token,apiKey) != HttpStatus.OK){
                log.error("The error occurred while the user was being assigned a card");
                throw new CommentNotSavedException("An error occurred with saving");
            }
        }catch (NullPointerException | CommentNotSavedException e){
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            throw e;
        }

    }

    @Transactional
    public void addComment(Long cardId, CommentDto commentDto, String authorization) throws InvalidDataException, CardNotFoundException, TokenNotExistException {
        String token = authorization.substring(7);
        checkToken(token);
        try {
            String commentText = commentDto.text();

            if (commentText.trim().isEmpty()){
                throw new InvalidDataException("The text must be present");
            }

            CardModel cardModel = cardRepo.getCardModelById(cardId)
                    .orElseThrow(()->new CardNotFoundException("Card with this id doesn't exist"));

            UserDtoRequest user = requestToGetUserByToken(token);

            CommentModel commentModel = CommentModel.builder()
                    .text(commentDto.text())
                    .cardId(cardModel.getId())
                    .userId(user.id())
                    .createdAt(LocalDateTime.now())
                    .build();

            commentRepo.save(commentModel);

            requestToLinkCommentAndUser(commentModel.getId(),token);

            invalidateCardCommentsCache(cardId);
        } catch (InvalidDataException | CardNotFoundException e){
            throw e;
        } catch (Exception e){
            log.error("An unknown error occurred while adding comment: "+e.getMessage()+" - "+e.getClass());
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }

    private List<CommentDtoResponse> getCachedComments(Page<CommentModel> comments,String token,String keyForCache){
        if (comments.isEmpty()){// Если комментариев вообще нет
            return List.of();//Возвращаем пустой список
        }

        List<CommentDtoResponse> listOfComments = new ArrayList<>();

        comments.stream().parallel()
                .forEach(comment->{
                    CommentAuthorDto authorDto;
                    try{
                        authorDto = new CommentAuthorDto(requestToGetUserById(token,comment.getUserId()).name());
                    }catch (UsernameNotFoundException e){
                        throw new RuntimeException(e.getMessage());
                    }
                    CommentDtoResponse commentDtoResponse = new CommentDtoResponse(
                            comment.getId(),
                            comment.getText(),
                            comment.getCreatedAt(),
                            authorDto,
                            comment.getReplyComments().size());
                    try {
                        redisCommands.rpush(keyForCache, objectMapper.writeValueAsString(commentDtoResponse));
                        listOfComments.add(commentDtoResponse);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(new SerializationException("An error occurred during serialization"));
                    }
                });

        redisCommands.expire(keyForCache, 60); // Устанавливаем время жизни равное минуте

        return listOfComments;
    }

    public List<CommentDtoResponse> getComments(Long cardId,int limit,int page, String authorization) throws CardNotFoundException, SerializationException, TokenNotExistException {
        String token = authorization.substring(7);
        checkToken(token);
        try {
            if (cardRepo.existsById(cardId)){// Проверяем существует ли такая карточка
                String commentsKeyForCache = "comments:card:" + cardId; // Ключ в редисе от списка комментариев
                Page<CommentModel> comments = commentRepo.getCommentModelByCardIdAndParentCommentIsNull(cardId, PageRequest.of(page,limit));
                return cacheComments(comments,commentsKeyForCache,token,limit);
            }else {
                throw new CardNotFoundException("Card with this id doesn't exist");
            }
        } catch (CardNotFoundException | SerializationException e){
            throw e;
        } catch (Exception e){
            log.debug("An unknown error occurred while deleting the card: "+e.getMessage()+" - "+e.getClass());
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }

    private List<CommentDtoResponse> cacheComments(Page<CommentModel> comments,String commentsKeyForCache, String token, int limit) throws SerializationException {
        List<String> cachedComments = redisCommands.lrange(commentsKeyForCache, 0, limit - 1);// Проверяем есть ли закешированные результаты

        if (cachedComments.isEmpty()){// Если кеша нет
            return getCachedComments(comments,token,commentsKeyForCache);

        }else {// Если кеш найден
            return cachedComments.stream()
                    .map(comment-> {
                        try {
                            return objectMapper.readValue(comment,CommentDtoResponse.class);// Десирализуем текст в классы и складываем в лист
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(new SerializationException("An error occurred during deserialization"));
                        }
                    }).toList();
        }
    }

    @Transactional
    public void deleteAllCommentsByCard(Long cardId, String authorization) throws TokenNotExistException {
        String token = authorization.substring(7);
        checkToken(token);
        try {
            List<CommentModel> comments = commentRepo.getCommentModelByCardId(cardId);
            invalidateCardCommentsCache(cardId);
            comments.forEach(comment->invalidateReplyCommentsCacheForChainIterative(comment.getId()));

            if (!comments.isEmpty()){
                comments.parallelStream().forEach(comment->{
                    try {
                        requestToUnlinkCommentFromUser(token,comment.getId(),comment.getUserId());
                    } catch (CommentNotUnlinkException e){
                        throw new RuntimeException(e.getMessage());
                    }
                });
                commentRepo.deleteAllByCardId(cardId);
            }
        } catch (Exception e){
            log.debug("An unknown error occurred while deleting the card: "+e.getMessage()+" - "+e.getClass());
            throw new RuntimeException("Unexpected error occurred", e);
        }

    }

    private void checkPermissions(CommentModel commentModel, String token) throws UsernameNotFoundException, NotEnoughPermissionsException {
        Long commentAuthorId = commentModel.getUserId();
        UserDtoRequest user = requestToGetUserByToken(token);

        if (!commentAuthorId.equals(user.id()) && user.role().stream().noneMatch(role->role.equals("ROLE_ADMIN"))){
            throw new NotEnoughPermissionsException("You don't have permission to do this");
        }
    }

    @Transactional
    public void patchComment(Long commentId, CommentDto commentDto, String authorization) throws InvalidDataException, CommentNotFoundException, TokenNotExistException {
        String token = authorization.substring(7);
        checkToken(token);
        try {
            String commentText = commentDto.text();

            if (commentText.trim().isEmpty()){
                throw new InvalidDataException("The text must be present");
            }

            CommentModel commentModel = commentRepo.getCommentModelById(commentId)
                    .orElseThrow(()->new CommentNotFoundException("Card with this id doesn't exist"));

            checkPermissions(commentModel,token);

            commentModel.setText(commentText);

            commentRepo.save(commentModel);
            invalidateCardCommentsCache(commentModel.getCardId());
            invalidateReplyCommentsCacheForChainIterative(commentId);
        } catch (InvalidDataException | CommentNotFoundException e){
            throw e;
        } catch (Exception e){
            log.debug("An unknown error occurred while deleting the card: "+e.getMessage()+" - "+e.getClass());
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }

    @Transactional
    public void deleteOneComment(Long commentId, String authorization) throws TokenNotExistException, CommentNotFoundException, UsernameNotFoundException, NotEnoughPermissionsException, CommentNotUnlinkException {
        String token = authorization.substring(7);
        checkToken(token);
        CommentModel commentModel = commentRepo.getCommentModelById(commentId)
                .orElseThrow(()->new CommentNotFoundException("Card with this id doesn't exist"));
        checkPermissions(commentModel,token);
        requestToUnlinkCommentFromUser(token,commentId,commentModel.getUserId());
        commentRepo.deleteById(commentId);
        invalidateCardCommentsCache(commentModel.getCardId());
        invalidateReplyCommentsCacheForChainIterative(commentId);
    }

    @Transactional
    public void replyComment(Long commentId, CommentDto commentDto, String authorization) throws InvalidDataException, TokenNotExistException, CommentNotFoundException, UsernameNotFoundException, CommentNotSavedException {
        String token = authorization.substring(7);
        checkToken(token);

        String commentText = commentDto.text();

        if (commentText.trim().isEmpty()){
            throw new InvalidDataException("The text must be present");
        }

        CommentModel parentComment = commentRepo.getCommentModelById(commentId)
                .orElseThrow(()->new CommentNotFoundException("Comment with this id doesn't exist"));

        UserDtoRequest user = requestToGetUserByToken(token);

        CommentModel commentModel = CommentModel.builder()
                .text(commentDto.text())
                .cardId(parentComment.getCardId())
                .parentComment(parentComment)
                .userId(user.id())
                .createdAt(LocalDateTime.now())
                .build();

        commentRepo.save(commentModel);

        requestToLinkCommentAndUser(commentModel.getId(),token);
        invalidateCardCommentsCache(commentModel.getCardId());
        invalidateReplyCommentsCacheForChainIterative(commentId);
    }

    public List<CommentDtoResponse> getReplyComments(Long commentId, int limit, int page, String authorization) throws TokenNotExistException, SerializationException, CommentNotFoundException {
        String token = authorization.substring(7);
        checkToken(token);
        if (commentRepo.existsById(commentId)){
            String keyForCache = "comments:reply:" + commentId;
            Page<CommentModel> comments = commentRepo.getCommentModelsByParentComment_Id(commentId, PageRequest.of(page,limit));
            return cacheComments(comments,keyForCache,token,limit);
        }else {
            throw new CommentNotFoundException("Comment with this id doesn't exist");
        }
    }

    private void invalidateCardCommentsCache(Long cardId) {
        String pattern = "comments:card:" + cardId + "*";
        List<String> keys = redisCommands.keys(pattern);
        if (!keys.isEmpty()) {
            redisCommands.del(keys.toArray(new String[0]));
        }
    }

    private void invalidateReplyCommentsCacheForChainIterative(Long commentId) {
        Long currentCommentId = commentId;
        while (currentCommentId != null) {
            invalidateReplyCommentsCache(currentCommentId);
            Optional<Long> parentOpt = commentRepo.getParentCommentId(currentCommentId);
            currentCommentId = parentOpt.orElse(null);
        }
    }

    private void invalidateReplyCommentsCache(Long commentId) {
        String pattern = "comments:reply:" + commentId + "*";
        List<String> keys = redisCommands.keys(pattern);
        log.debug("Invalidating keys for pattern {}: {}", pattern, keys);
        if (!keys.isEmpty()) {
            redisCommands.del(keys.toArray(new String[0]));
        }
    }
}