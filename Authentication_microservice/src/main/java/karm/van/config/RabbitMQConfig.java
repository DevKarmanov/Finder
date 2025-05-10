package karm.van.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    @Value("${rabbitmq.queue.recovery.name}")
    public String recoveryQueue;

    @Value("${rabbitmq.exchange.message.name}")
    public String exchange;

    @Value("${rabbitmq.routing-key.recovery.name}")
    public String recoveryRoutingKey;

    @Bean
    public Queue emailQueue() {
        return new Queue(recoveryQueue);
    }

    @Bean
    public TopicExchange finderExchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    public Binding emailMessageBinding() {
        return BindingBuilder.bind(emailQueue()).to(finderExchange()).with(recoveryRoutingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate();
        rabbitTemplate.setConnectionFactory(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
