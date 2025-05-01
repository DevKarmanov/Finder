package karm.van.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.api.sync.RedisStringCommands;
import karm.van.config.properties.AuthenticationMicroServiceProperties;
import karm.van.config.properties.CommentMicroServiceProperties;
import karm.van.config.properties.ImageMicroServiceProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@EnableJpaRepositories(basePackages = "karm.van.repo.jpaRepo")
@EnableElasticsearchRepositories(basePackages = "karm.van.repo.elasticRepo")
@EnableConfigurationProperties({CommentMicroServiceProperties.class, ImageMicroServiceProperties.class, AuthenticationMicroServiceProperties.class})
@Configuration
@EnableAsync
public class AdsConfiguration {

    @Value("${redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Bean
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Async-");
        executor.initialize();
        return executor;
    }

    @Bean(destroyMethod = "close")
    public RedisClient redisClient() {
        RedisURI redisURI = RedisURI.builder()
                .withHost(redisHost)
                .withPort(6379)
                .withPassword(redisPassword.toCharArray())
                .withDatabase(0)
                .build();
        RedisClient client = RedisClient.create(redisURI);
        client.setOptions(ClientOptions.builder()
                .autoReconnect(true)
                .build());
        return client;
    }

    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<String, String> redisConnection(RedisClient redisClient) {
        return redisClient.connect();
    }

    @Bean
    public RedisCommands<String, String> redisCommands(StatefulRedisConnection<String, String> connection) {
        return connection.sync();
    }

}
