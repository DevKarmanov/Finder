package karm.van.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "microservices.auth")
@Getter
@Setter
public class AuthMicroServiceProperties {
    private String prefix;
    private String host;
    private Endpoints endpoints;

    @Setter
    @Getter
    public static class Endpoints{
        private String recoveryPassword;
    }
}
