package karm.van.service;

import karm.van.dto.elastic.UserRequest;
import karm.van.dto.rollBack.RollBackCommand;
import karm.van.model.MyUser;
import karm.van.model.MyUserDocument;
import karm.van.repo.elasticRepo.ElasticRepo;
import karm.van.service.rollBack.RollbackHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableRetry
public class ConsumerService {
    private final Map<String, RollbackHandler> handlers;
    private final ElasticRepo elasticRepo;

    @Transactional
    @Retryable(backoff = @Backoff(delay = 10000))
    @RabbitListener(queues = "${rabbitmq.queue.elastic.save.name}")
    public void elasticSaveDocumentConsume(MyUserDocument userDocument) {
        try {
            elasticRepo.save(userDocument);
            log.info("User successfully saved to elastic");
        }catch (Exception e){
            log.error("Error saving the user in elasticRepo");
            throw new RuntimeException();
        }

    }

    @Transactional
    @Retryable(backoff = @Backoff(delay = 10000))
    @RabbitListener(queues = "${rabbitmq.queue.elastic.del.name}")
    public void elasticDelDocumentConsume(MyUser user) {
        try {
            elasticRepo.findById(user.getId())
                    .ifPresent(elasticRepo::delete);
            log.info("User successfully deleted to elastic");
        }catch (Exception e){
            log.error("Error deleting the user in elasticRepo");
            throw new RuntimeException();
        }

    }


    @Transactional
    @Retryable(backoff = @Backoff(delay = 10000))
    @RabbitListener(queues = "${rabbitmq.queue.elastic.patch.name}")
    public void elasticPatchDocumentConsume(UserRequest userRequest) {
        System.out.println("ПОЛУЧЕНО PATCH");
        try {
            elasticRepo.findById(userRequest.id())
            .ifPresentOrElse(user->{
                userRequest.name().ifPresent(name -> {
                    if (!name.trim().isEmpty()) {
                        user.setName(name.trim());
                    }
                });

                userRequest.firstName().ifPresent(firstName -> {
                    if (!firstName.trim().isEmpty()) {
                        user.setFirstName(firstName.trim());
                    }
                });

                userRequest.lastName().ifPresent(lastName -> {
                    if (!lastName.trim().isEmpty()) {
                        user.setLastName(lastName.trim());
                    }
                });

                userRequest.description().ifPresent(description -> {
                    if (!description.trim().isEmpty()) {
                        user.setDescription(description.trim());
                    }
                });

                userRequest.country().ifPresent(country -> {
                    if (!country.trim().isEmpty()) {
                        user.setCountry(country.trim());
                    }
                });

                userRequest.roleInCommand().ifPresent(role -> {
                    if (!role.trim().isEmpty()) {
                        user.setRoleInCommand(role.trim());
                    }
                });

                userRequest.skills().ifPresent(skills -> {
                    if (!skills.trim().isEmpty()) {
                        user.setSkills(skills.trim());
                    }
                });

                elasticRepo.save(user);
                log.info("Successfully patched user document with id {}", user.getId());
            },()-> System.out.println("ПОЛЬЗОВАТЕЛЬ ДЛЯ PATCH НЕ НАЙДЕН: "+userRequest));
        } catch (Exception e) {
            log.error("Error patching the user in elasticRepo", e);
            throw new RuntimeException("Failed to patch user", e);
        }
    }

    @Retryable(backoff = @Backoff(delay = 10000))
    @RabbitListener(queues = "${rabbitmq.queue.rollback.name}")
    public void rollBack(RollBackCommand command) {
        RollbackHandler handler = handlers.get(command.rollbackType());
        if (handler == null) {
            log.warn("No rollback handler found for type: {}", command.rollbackType());
            return;
        }
        handler.handle(command.params());
    }

}
