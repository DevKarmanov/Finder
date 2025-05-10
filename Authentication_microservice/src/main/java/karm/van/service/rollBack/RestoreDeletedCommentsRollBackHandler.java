package karm.van.service.rollBack;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import karm.van.config.properties.CommentMicroServiceProperties;
import karm.van.dto.response.CommentDto;
import karm.van.service.ApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component("RestoreDeletedComments")
@Slf4j
public class RestoreDeletedCommentsRollBackHandler implements RollbackHandler{
    private final ApiService apiService;
    private final CommentMicroServiceProperties commentProperties;
    private final ObjectMapper objectMapper;

    @Value("${microservices.x-api-key}")
    private String apiKey;

    public RestoreDeletedCommentsRollBackHandler(ApiService apiService, CommentMicroServiceProperties commentProperties, ObjectMapper objectMapper) {
        this.apiService = apiService;
        this.commentProperties = commentProperties;
        this.objectMapper = objectMapper;
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void handle(Map<String, Object> params) {
        String url = apiService.buildUrl(
                commentProperties.getPrefix(),
                commentProperties.getHost(),
                commentProperties.getPort(),
                commentProperties.getEndpoints().getAddComments());
        try {
            List<CommentDto> commentDtos = objectMapper.readValue((String) params.get("listOfComments"),new TypeReference<>() {});

            var status = apiService.requestToRollBackDeletedComments(url,apiKey,commentDtos);
            if (status!= HttpStatus.OK){
                log.error("The error occurred while the user was being assigned a card");
                throw new RuntimeException();
            }
            log.info("RestoreDeletedComments was successful");
        }catch (Exception e){
            log.error("An error occurred while restoring deleted comments: {}",e.getMessage());
            throw new RuntimeException("RollBack failed");
        }

    }
}
