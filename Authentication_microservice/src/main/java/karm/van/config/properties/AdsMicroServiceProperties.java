package karm.van.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "microservices.ads")
@Getter
@Setter
public class AdsMicroServiceProperties {
    private String prefix;
    private String host;
    private String port;
    private Endpoints endpoints;

    @Setter
    @Getter
    public static class Endpoints{
        private String delCard;
        private String getUserCards;
        private String delAllComplaintByUserId;
    }
}
