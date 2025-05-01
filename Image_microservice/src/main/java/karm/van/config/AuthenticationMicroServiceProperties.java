package karm.van.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "microservices.authentication")
@Getter
@Setter
public class AuthenticationMicroServiceProperties {
    private String prefix;
    private String host;
    private String port;
    private Endpoints endpoints;

    @Setter
    @Getter
    public static class Endpoints{
        private String validateToken;
        private String addProfileImage;
        private String user;
    }
}
