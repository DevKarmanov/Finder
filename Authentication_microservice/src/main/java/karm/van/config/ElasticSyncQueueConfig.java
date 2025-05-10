package karm.van.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ElasticSyncQueueConfig {

    @Value("${rabbitmq.queue.elastic.save.name}")
    public String elasticQueueSave;

    @Value("${rabbitmq.queue.elastic.del.name}")
    public String elasticQueueDel;

    @Value("${rabbitmq.queue.elastic.patch.name}")
    public String elasticQueuePatch;

    @Value("${rabbitmq.routing-key.elastic.save.name}")
    public String elasticRoutingKeySave;

    @Value("${rabbitmq.routing-key.elastic.del.name}")
    public String elasticRoutingKeyDel;

    @Value("${rabbitmq.routing-key.elastic.patch.name}")
    public String elasticRoutingKeyPatch;

    @Value("${rabbitmq.exchange.elastic.name}")
    public String exchange;

    @Bean
    public TopicExchange elasticExchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    public Queue elasticQueueSave(){
        return new Queue(elasticQueueSave);
    }

    @Bean
    public Queue elasticQueueDel(){
        return new Queue(elasticQueueDel);
    }

    @Bean
    public Queue elasticQueuePatch(){
        return new Queue(elasticQueuePatch);
    }

    @Bean
    public Binding elasticSaveBinding(){
        return BindingBuilder
                .bind(elasticQueueSave())
                .to(elasticExchange())
                .with(elasticRoutingKeySave);
    }

    @Bean
    public Binding elasticDelBinding(){
        return BindingBuilder
                .bind(elasticQueueDel())
                .to(elasticExchange())
                .with(elasticRoutingKeyDel);
    }

    @Bean
    public Binding elasticPatchBinding(){
        return BindingBuilder
                .bind(elasticQueuePatch())
                .to(elasticExchange())
                .with(elasticRoutingKeyPatch);
    }

}