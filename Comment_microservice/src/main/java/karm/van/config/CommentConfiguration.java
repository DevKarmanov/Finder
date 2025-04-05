package karm.van.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import redis.clients.jedis.Jedis;

import java.util.concurrent.Executor;

@EnableConfigurationProperties(AuthenticationMicroServiceProperties.class)
@Configuration
public class CommentConfiguration {
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
