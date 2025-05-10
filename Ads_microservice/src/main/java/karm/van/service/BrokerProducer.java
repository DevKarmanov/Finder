package karm.van.service;

import karm.van.dto.card.ElasticPatchDto;
import karm.van.dto.message.EmailDataDto;
import karm.van.dto.rollBack.RollBackCommand;
import karm.van.model.CardDocument;
import karm.van.model.CardModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrokerProducer {
    @Value("${rabbitmq.exchange.message.name}")
    private String finderExchange;

    @Value("${rabbitmq.exchange.elastic.name}")
    private String elasticExchange;

    @Value("${rabbitmq.exchange.rollback.name}")
    private String rollBackExchange;

    @Value("${rabbitmq.routing-key.elastic.save.name}")
    public String elasticRoutingKeySave;

    @Value("${rabbitmq.routing-key.elastic.del.name}")
    public String elasticRoutingKeyDel;

    @Value("${rabbitmq.routing-key.elastic.patch.name}")
    public String elasticRoutingKeyPatch;

    @Value("${rabbitmq.routing-key.email.name}")
    private String emailRoutingKey;

    @Value("${rabbitmq.routing-key.rollback.name}")
    private String rollBackRoutingKey;

    private final RabbitTemplate rabbitTemplate;

    public void sendEmailMessage(EmailDataDto emailDataDto){
        rabbitTemplate.convertAndSend(finderExchange,emailRoutingKey,emailDataDto);
    }

    public void saveInBroker(CardDocument cardDocument){
        rabbitTemplate.convertAndSend(elasticExchange,elasticRoutingKeySave,cardDocument);
    }

    public void saveInBroker(CardModel cardModel){
        rabbitTemplate.convertAndSend(elasticExchange,elasticRoutingKeyDel,cardModel);
    }

    public void saveInBroker(ElasticPatchDto elasticPatchDto){
        rabbitTemplate.convertAndSend(elasticExchange,elasticRoutingKeyPatch,elasticPatchDto);
    }

    public void sendRollBack(RollBackCommand rollBackCommand){
        rabbitTemplate.convertAndSend(rollBackExchange,rollBackRoutingKey,rollBackCommand);
    }

}
