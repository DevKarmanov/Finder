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

@Component("DeleteSavedImages")
@Slf4j
public class DeleteSavedImagesRollBackHandler implements RollbackHandler{
    private final ApiService apiService;
    private final ImageMicroServiceProperties imageProperties;

    @Value("${microservices.x-api-key}")
    private String apiKey;

    public DeleteSavedImagesRollBackHandler(ApiService apiService, ImageMicroServiceProperties imageProperties) {
        this.apiService = apiService;
        this.imageProperties = imageProperties;
    }

    @Override
    public void handle(Map<String, Object> params) {

        List<Long> imageIds = (List<Long>) params.get("listOfImageIds");

        HttpStatusCode result;

        try {
            result = apiService.sendDeleteImagesFromMinioRequest(apiService.buildUrl(
                    imageProperties.getPrefix(),
                    imageProperties.getHost(),
                    imageProperties.getPort(),
                    imageProperties.getEndpoints().getDelImagesFromMinio()
            ), imageIds, apiKey);
            log.info("DeleteSavedImages was successful");
        } catch (NullPointerException e) {
            log.error("Method sendDeleteImagesFromMinioRequest returned the value null");
            throw e;
        }

        if (result != HttpStatus.OK) {
            log.error("An error occurred while deleting images");
            throw new RuntimeException("RollBack failed");
        }

    }
}
