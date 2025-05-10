package karm.van.service.rollBack;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import karm.van.config.properties.AuthenticationMicroServiceProperties;
import karm.van.exception.card.CardNotSavedException;
import karm.van.model.CardModel;
import karm.van.service.ApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("CardAndUserLink")
@Slf4j
public class LinkCardAndUserRollBackHandler implements RollbackHandler{
    private final ApiService apiService;
    private final AuthenticationMicroServiceProperties authenticationProperties;
    private final ObjectMapper mapper;

    @Value("${microservices.x-api-key}")
    private String apiKey;

    public LinkCardAndUserRollBackHandler(ApiService apiService, AuthenticationMicroServiceProperties authenticationProperties, ObjectMapper mapper) {
        this.apiService = apiService;
        this.authenticationProperties = authenticationProperties;
        this.mapper = mapper;
        mapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void handle(Map<String, Object> params) {
        CardModel cardModel = mapper.convertValue(params.get("cardModel"), CardModel.class);
        Long userId = ((Number) params.get("userId")).longValue();

        String url = apiService.buildUrl(
                authenticationProperties.getPrefix(),
                authenticationProperties.getHost(),
                authenticationProperties.getPort(),
                authenticationProperties.getEndpoints().getAddCardToUser(),
                cardModel.getId(),userId);

        HttpStatusCode result;

        try {
            result = apiService.addCardToUser(url, apiKey);
            log.info("LinkCardAndUserRollBack was successful");
        } catch (NullPointerException e) {
            log.error("Method addCardToUser returned the value null");
            throw e;
        }

        if (result != HttpStatus.OK) {
            log.error("The error occurred while the user was being assigned a card");
            throw new RuntimeException("RollBack failed");
        }
    }
}
