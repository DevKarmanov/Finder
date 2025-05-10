package karm.van.service.rollBack;

import karm.van.config.properties.ImageMicroServiceProperties;
import karm.van.service.ApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component("MoveImagesToImagePackage")
@Slf4j
public class MoveImagesToImagePackageRollBackHandler implements RollbackHandler{
    private final ApiService apiService;
    private final ImageMicroServiceProperties imageProperties;

    @Value("${microservices.x-api-key}")
    private String apiKey;

    public MoveImagesToImagePackageRollBackHandler(ApiService apiService, ImageMicroServiceProperties imageProperties) {
        this.apiService = apiService;
        this.imageProperties = imageProperties;
    }

    @Override
    public void handle(Map<String, Object> params) {
        String imageUrl = apiService.buildUrl(
                imageProperties.getPrefix(),
                imageProperties.getHost(),
                imageProperties.getPort(),
                imageProperties.getEndpoints().getMoveImage());

        List<Long> imagesId = (List<Long>) params.get("listOfImageIds");

        HttpStatusCode result;

        try {
            result = apiService.moveImagesToImagePackage(imageUrl, imagesId, apiKey);
            log.info("MoveImagesToImagePackageRollBack was successful");
        } catch (NullPointerException e) {
            log.error("Method moveImagesToImagePackage returned the value null");
            throw e;
        }

        if (result != HttpStatus.OK) {
            log.error("The error occurred while the user was being assigned a card");
            throw new RuntimeException("RollBack failed");
        }
    }
}
