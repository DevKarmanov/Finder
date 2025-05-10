package karm.van.service;

import karm.van.dto.elastic.UserRequest;
import karm.van.dto.request.UserPatchRequest;
import karm.van.dto.response.RecoveryMessageDto;
import karm.van.dto.rollBack.RollBackCommand;
import karm.van.model.MyUser;
import karm.van.model.MyUserDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {
    @Value("${rabbitmq.exchange.message.name}")
    private String exchangeName;

    @Value("${rabbitmq.exchange.elastic.name}")
    private String elasticExchange;

    @Value("${rabbitmq.routing-key.elastic.save.name}")
    public String elasticRoutingKeySave;

    @Value("${rabbitmq.routing-key.elastic.del.name}")
    public String elasticRoutingKeyDel;

    @Value("${rabbitmq.routing-key.elastic.patch.name}")
    public String elasticRoutingKeyPatch;

    @Value("${rabbitmq.exchange.rollback.name}")
    private String rollBackExchange;

    @Value("${rabbitmq.routing-key.rollback.name}")
    private String rollBackRoutingKey;

    @Value("${rabbitmq.routing-key.recovery.name}")
    private String recoveryRoutingKey;

    private final RabbitTemplate rabbitTemplate;

    public void sendRecoveryMessage(RecoveryMessageDto recoveryMessageDto){
        rabbitTemplate.convertAndSend(exchangeName,recoveryRoutingKey,recoveryMessageDto);
    }

    public void sendRollBack(RollBackCommand rollBackCommand){
        rabbitTemplate.convertAndSend(rollBackExchange,rollBackRoutingKey,rollBackCommand);
    }

    public void saveInElastic(MyUserDocument userDocument){
        rabbitTemplate.convertAndSend(elasticExchange,elasticRoutingKeySave,userDocument);
    }

    public void delInElastic(MyUser user){
        rabbitTemplate.convertAndSend(elasticExchange,elasticRoutingKeyDel,user);
    }

    public void patchInElastic(UserRequest userPatchRequest){
        rabbitTemplate.convertAndSend(elasticExchange,elasticRoutingKeyPatch,userPatchRequest);
    }

}
