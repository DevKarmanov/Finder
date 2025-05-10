package karm.van.service;

import karm.van.dto.card.CardDto;
import karm.van.dto.card.ElasticPatchDto;
import karm.van.dto.rollBack.RollBackCommand;
import karm.van.model.CardDocument;
import karm.van.model.CardModel;
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

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableRetry
public class ConsumerService {
    private final ElasticRepo elasticRepo;
    private final Map<String, RollbackHandler> handlers;

    @Transactional
    @Retryable(backoff = @Backoff(delay = 10000))
    @RabbitListener(queues = "${rabbitmq.queue.elastic.save.name}")
    public void elasticSaveDocumentConsume(CardDocument cardDocument) {
        try {
            elasticRepo.save(cardDocument);
        }catch (Exception e){
            log.error("Error saving the card in elasticRepo");
            throw new RuntimeException();
        }

    }

    @Transactional
    @Retryable(backoff = @Backoff(delay = 10000))
    @RabbitListener(queues = "${rabbitmq.queue.elastic.del.name}")
    public void elasticDelDocumentConsume(CardModel cardModel) {
        try {
            elasticRepo.findById(cardModel.getId())
                    .ifPresent(elasticRepo::delete);
        }catch (Exception e){
            log.error("Error deleting the card in elasticRepo");
            throw new RuntimeException();
        }

    }


    @Transactional
    @Retryable(backoff = @Backoff(delay = 10000))
    @RabbitListener(queues = "${rabbitmq.queue.elastic.patch.name}")
    public void elasticPatchDocumentConsume(ElasticPatchDto elasticPatchDto) {
        try {
            CardDto cardDto = elasticPatchDto.cardDto();

            String title = cardDto.title();
            String text = cardDto.text();
            List<String> tags = cardDto.tags();

            elasticRepo.findById(elasticPatchDto.id())
                    .ifPresent(cardDocument -> {
                        if (!title.trim().isEmpty()){
                            cardDocument.setTitle(title);
                        }
                        if (text.trim().isEmpty()){
                            cardDocument.setText(text);
                        }
                        cardDocument.setTags(tags);

                        elasticRepo.save(cardDocument);
                    });
        }catch (Exception e){
            log.error("Error patching the card in elasticRepo");
            throw new RuntimeException();
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
