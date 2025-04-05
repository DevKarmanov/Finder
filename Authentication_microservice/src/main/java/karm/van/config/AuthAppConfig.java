package karm.van.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.Jedis;

@Configuration
public class AuthAppConfig {
    @Value("${redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Bean
    public Jedis jedis(){
        Jedis jedis = new Jedis(redisHost,6379);
        jedis.auth(redisPassword);
        return jedis;
    }
}
