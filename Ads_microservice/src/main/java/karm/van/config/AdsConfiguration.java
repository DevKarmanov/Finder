package karm.van.config;

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
import redis.clients.jedis.Jedis;

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

    @Bean
    public Jedis jedis(){
        Jedis jedis = new Jedis(redisHost,6379);
        jedis.auth(redisPassword);
        return jedis;
    }

}
