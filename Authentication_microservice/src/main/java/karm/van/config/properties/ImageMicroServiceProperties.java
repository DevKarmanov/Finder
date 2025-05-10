package karm.van.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "microservices.image")
@Getter
@Setter
public class ImageMicroServiceProperties {
    private String prefix;
    private String host;
    private String port;
    private Endpoints endpoints;

    @Setter
    @Getter
    public static class Endpoints{
        private String saveProfileImage;
        private String moveImage;
        private String delImageFromMinio;
        private String moveProfileImage;
        private String profileImage;
    }
}
