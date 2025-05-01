package karm.van.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.api.sync.RedisCommands;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ElasticService {
    private final RedisCommands<String, String> redisCommands;
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

    public CardPageResponseDto search(String query, int pageNumber, int limit, String authorization,
                                      Optional<LocalDate> createTimeOpt, Optional<List<String>> tagsOpt)
            throws SerializationException, TokenNotExistException {

        String token = authorization.substring(7);
        checkToken(token);

        StringBuilder redisKey = new StringBuilder("page:" + pageNumber + ":limit:" + limit + ":query:" + query);
        createTimeOpt.ifPresent(date -> redisKey.append(":date:").append(date));
        tagsOpt.ifPresent(tags -> redisKey.append(":tags:").append(String.join(",", tags)));

        String redisKeyStr = redisKey.toString();

        Long redisResult = redisCommands.exists(redisKeyStr);
        if (redisResult != null && redisResult > 0) {
            try {
                return objectMapper.readValue(redisCommands.get(redisKeyStr), CardPageResponseDto.class);
            } catch (JsonProcessingException e) {
                throw new SerializationException("Error while deserializing from Redis");
            }
        }

        PageRequest pageRequest = PageRequest.of(pageNumber, limit);
        Page<CardDocument> documents;

        boolean hasQuery = query != null && !query.isBlank();
        boolean hasDate = createTimeOpt.isPresent();
        boolean hasTags = tagsOpt.isPresent() && !tagsOpt.get().isEmpty();

        // лог
        System.out.println("=== [SEARCH REQUEST] ===");
        System.out.println("Query: " + query);
        System.out.println("Page: " + pageNumber + ", Limit: " + limit);
        System.out.println("Authorization token: " + token);
        System.out.println("CreateTime filter: " + (createTimeOpt.map(LocalDate::toString).orElse("none")));
        System.out.println("Tags filter: " + (hasTags ? String.join(", ", tagsOpt.get()) : "none"));
        System.out.println("Redis key: " + redisKeyStr);

        if (hasQuery) {
            if (hasDate && hasTags) {
                System.out.println("Search type: query + date + tags");
                documents = elasticRepo.findByQueryWithFilters(query, createTimeOpt.get().toString(), tagsOpt.get(), pageRequest);
            } else if (hasDate) {
                System.out.println("Search type: query + date");
                documents = elasticRepo.findByQueryAndDateOnly(query, createTimeOpt.get().toString(), pageRequest);
            } else if (hasTags) {
                System.out.println("Search type: query + tags");
                documents = elasticRepo.findByQueryWithFilters(query, "1970-01-01", tagsOpt.get(), pageRequest);
            } else {
                System.out.println("Search type: query only");
                documents = elasticRepo.findByQueryOnly(query, pageRequest);
            }
        } else {
            if (hasDate && hasTags) {
                System.out.println("Search type: date + tags (no query)");
                documents = elasticRepo.findByFiltersOnly(createTimeOpt.get().toString(), tagsOpt.get(), pageRequest);
            } else if (hasDate) {
                System.out.println("Search type: date only (no query)");
                documents = elasticRepo.findByDateOnly(createTimeOpt.get().toString(), pageRequest);
            } else if (hasTags) {
                System.out.println("Search type: tags only (no query)");
                documents = elasticRepo.findByTagsOnly(tagsOpt.get(), pageRequest);
            } else {
                System.out.println("Search type: no filters (return all)");
                documents = elasticRepo.findAll(pageRequest);
            }
        }

        System.out.println("Result count: " + documents.getTotalElements());

        List<Long> ids = documents.stream().map(CardDocument::getId).toList();
        List<CardModel> cards = cardRepo.findAllById(ids);

        return cacheCards(cards, documents, token, redisKeyStr);
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

        redisCommands.set(key,objectAsString);
        redisCommands.expire(key,60);

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
                                card.getTags(),
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
