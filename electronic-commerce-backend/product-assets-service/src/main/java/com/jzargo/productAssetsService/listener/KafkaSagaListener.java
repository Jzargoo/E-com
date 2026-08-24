package com.jzargo.productAssetsService.listener;

import com.jzargo.core.KafkaCustomHeaders;
import com.jzargo.core.command.createProductSaga.AssetsCompensationCommand;
import com.jzargo.core.command.createProductSaga.AssetsInitializationCommand;
import com.jzargo.productAssetsService.entity.Message;
import com.jzargo.productAssetsService.entity.MessageType;
import com.jzargo.productAssetsService.repository.MessageRepository;
import com.jzargo.productAssetsService.service.AssetsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@KafkaListener(
        topics = "#{kafkaPropertyStorage.topics.productCreateSaga.name}",
        properties = {"enable.auto.commit=false"},
        containerFactory = "manualListenerContainerFactory",
        groupId = "#{kafkaPropertyStorage.groupId}"
)
@Component
public class KafkaSagaListener {

    private final MessageRepository messageRepository;
    private final AssetsService assetsService;

    public KafkaSagaListener(MessageRepository messageRepository, AssetsService assetsService) {
        this.messageRepository = messageRepository;
        this.assetsService = assetsService;
    }

    @KafkaHandler
    public void handleSagaProductAssets(
            @Payload AssetsInitializationCommand command,
            @Header(KafkaCustomHeaders.IDEMPOTENCY_KEY) String messageId,
            Acknowledgment acknowledgment) {

        messageRepository.findById(messageId)

                .map(message -> true)

                .switchIfEmpty(Mono.just(false))

                .flatMap(
                        exists -> {

                            if (exists) {
                                logRepeatedMessage();

                                return Mono.empty();
                            }

                            Long productId = command.getProductId();
                            Integer shopId = command.getShopId();

                            return assetsService.initAssetsProduct(productId, shopId);
                        }
                )

                .flatMap(
                        productAssets -> messageRepository.save(
                                new Message(messageId, Instant.now(), MessageType.COMMAND)
                        )
                )

                .doOnSuccess(message -> acknowledgment.acknowledge())

                .subscribe();
    }

    @KafkaHandler
    @Transactional
    public void handleCompensationSagaProductAssets(
            @Payload AssetsCompensationCommand command,
            @Header(KafkaCustomHeaders.IDEMPOTENCY_KEY) String messageId,
            Acknowledgment acknowledgment) {

        messageRepository.findById(messageId)

                .map(message -> true)

                .switchIfEmpty(Mono.just(false))

                .flatMap(
                        exists -> {

                            if (exists) {

                                logRepeatedMessage();

                                return Mono.empty();

                            }

                            Long productId = command.getProductId();

                            return assetsService.initAssetsCompensation(productId);

                        }
                )

                .flatMap(
                        nothing -> messageRepository.save(
                                new Message(messageId, Instant.now(), MessageType.COMMAND)
                        )
                )

                .doOnSuccess(message -> acknowledgment.acknowledge())

                .subscribe();

    }

    private void logRepeatedMessage() {
        log.debug("Caught a repeated message!");
    }

    @KafkaHandler
    public void handle(Object ignored) {
        log.trace("Received command that is not published for assets service");
    }

}
