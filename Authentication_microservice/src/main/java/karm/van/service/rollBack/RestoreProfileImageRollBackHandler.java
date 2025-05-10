package karm.van.service.rollBack;

import karm.van.config.properties.ImageMicroServiceProperties;
import karm.van.service.ApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("RestoreProfileImage")
@Slf4j
public class RestoreProfileImageRollBackHandler implements RollbackHandler{
    private final ApiService apiService;
    private final ImageMicroServiceProperties imageProperties;

    @Value("${microservices.x-api-key}")
    private String apiKey;

    public RestoreProfileImageRollBackHandler(ApiService apiService, ImageMicroServiceProperties imageProperties) {
        this.apiService = apiService;
        this.imageProperties = imageProperties;
    }

    @Override
    public void handle(Map<String, Object> params) {
        Long imageId = ((Number)params.get("imageId")).longValue();
        String imageUrl = apiService.buildUrl(
                imageProperties.getPrefix(),
                imageProperties.getHost(),
                imageProperties.getPort(),
                imageProperties.getEndpoints().getMoveProfileImage(),
                imageId
        );

        HttpStatusCode result;

        try {
            result = apiService.moveProfileImage(imageUrl, apiKey,false);
            log.info("RestoreProfileImageRollBck was successful");
        } catch (NullPointerException e) {
            log.error("Method moveProfileImage returned the value null");
            throw e;
        }

        if (result != HttpStatus.OK) {
            log.error("An error occurred while moving the profile image");
            throw new RuntimeException("RollBack failed");
        }
    }
}
