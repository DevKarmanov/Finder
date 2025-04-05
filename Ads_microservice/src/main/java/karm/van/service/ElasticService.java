package karm.van.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import karm.van.config.properties.AuthenticationMicroServiceProperties;
import karm.van.config.properties.ImageMicroServiceProperties;
import karm.van.dto.card.CardPageResponseDto;
import karm.van.dto.card.FullCardDtoForOutput;
import karm.van.dto.image.ImageDto;
import karm.van.dto.user.UserDtoRequest;
import karm.van.exception.other.SerializationException;
import karm.van.exception.other.TokenNotExistException;
import karm.van.exception.user.UsernameNotFoundException;
import karm.van.model.CardDocument;
import karm.van.model.CardModel;
import karm.van.repo.elasticRepo.ElasticRepo;
import karm.van.repo.jpaRepo.CardRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ElasticService {
    private final Jedis redis;
    private final ObjectMapper objectMapper;
    private final ElasticRepo elasticRepo;
    private final CardRepo cardRepo;
    private final ApiService apiService;
    private final ImageMicroServiceProperties imageProperties;
    private final AuthenticationMicroServiceProperties authenticationProperties;

    @Value("${microservices.x-api-key}")
    private String apiKey;

    private void checkToken(String token) throws TokenNotExistException {
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

    public CardPageResponseDto search(String query, int pageNumber, int limit, String authorization, Optional<LocalDate> createTime) throws SerializationException, TokenNotExistException {
        String token = authorization.substring(7);
        checkToken(token);

        StringBuilder redisKey = new StringBuilder("pageNumber:" + pageNumber + ":limit:" + limit + ":" + query);
        createTime.map(timeFilter-> redisKey.append(":").append(timeFilter));

        if (redis.exists(String.valueOf(redisKey))){
            try {
                return objectMapper.readValue(redis.get(String.valueOf(redisKey)), CardPageResponseDto.class);
            } catch (JsonProcessingException e) {
                throw new SerializationException("an error occurred during serialization");
            }
        }else {
            PageRequest pageRequest = PageRequest.of(pageNumber,limit);

            Page<CardDocument> documents = createTime.map(timeFilter->
                    elasticRepo.findByQueryAndSortByData(query,timeFilter.toString(),pageRequest))
                    .orElseGet(()-> elasticRepo.findByQuery(query, pageRequest));

            List<Long> ids = documents.stream()
                    .map(CardDocument::getId)
                    .toList();

            List<CardModel> cards = cardRepo.findAllById(ids);
            return cacheCards(cards,documents,token, String.valueOf(redisKey));
        }

    }

    private CardPageResponseDto cacheCards(List<CardModel> cards,Page<CardDocument> page, String token, String key) throws SerializationException {
        String objectAsString;

        CardPageResponseDto cardPageResponseDto = new CardPageResponseDto(
                getFullCardsDto(token,cards),
                page.isLast(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.isFirst(),
                page.getNumberOfElements());

        try {
            objectAsString = objectMapper.writeValueAsString(cardPageResponseDto);
        } catch (JsonProcessingException e) {
            throw new SerializationException("an error occurred during deserialization");
        }

        redis.set(key,objectAsString);
        redis.expire(key,60);

        return cardPageResponseDto;
    }

    private List<FullCardDtoForOutput> getFullCardsDto(String token, List<CardModel> page){
        return page.stream()
                .map(card -> {
                    try {
                        return new FullCardDtoForOutput(
                                card.getId(),
                                card.getTitle(),
                                card.getText(),
                                card.getCreateTime(),
                                requestToGetAllCardImages(card,token),
                                requestToGetUserById(token,card.getUserId()).name());
                    } catch (UsernameNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }).toList();
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

    private UserDtoRequest requestToGetUserById(String token, Long userId) throws UsernameNotFoundException {
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
}
