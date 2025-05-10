package karm.van.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RollBackQueueConfig {

    private final String rollbackQueue;
    private final String rollBackRoutingKey;
    private final String rollBackExchange;

    public RollBackQueueConfig(
            @Value("${rabbitmq.queue.rollback.name}") String rollbackQueue,
            @Value("${rabbitmq.routing-key.rollback.name}") String rollBackRoutingKey,
            @Value("${rabbitmq.exchange.rollback.name}") String rollBackExchange
    ) {
        this.rollbackQueue = rollbackQueue;
        this.rollBackRoutingKey = rollBackRoutingKey;
        this.rollBackExchange = rollBackExchange;
    }

    @Bean
    public TopicExchange rollBackExchange() {
        return new TopicExchange(rollBackExchange);
    }

    @Bean
    public Queue rollbackQueue(){
        return new Queue(rollbackQueue);
    }


    @Bean
    public Binding rollbackCardBinding(){
        return BindingBuilder
                .bind(rollbackQueue())
                .to(rollBackExchange())
                .with(rollBackRoutingKey);
    }
}