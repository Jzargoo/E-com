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

import java.time.Instant;

@Slf4j
@KafkaListener(
        topics = "",
        properties = {"spring.kafka.enable.auto.commit=false"},
        groupId = "#{kafkaPropertyStorage}"
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

        if (
                messageRepository.findById(messageId)
                        .isPresent()
        ) {
            logRepeatedMessage();
            return;
        }

        try {

            assetsService.initAssetsProduct(
                    command.getProductId()
            );

            messageRepository.save(
                    new Message(messageId, Instant.now(), MessageType.COMMAND)
            );

            acknowledgment.acknowledge();

        } catch (Exception e) {

            log.error("Occurred exception when processing saga product assets command. " +
                    "Acknowledgement was not updated", e
            );

        }

    }

    @KafkaHandler
    @Transactional
    public void handleCompensationSagaProductAssets(
            @Payload AssetsCompensationCommand command,
            @Header(KafkaCustomHeaders.IDEMPOTENCY_KEY) String messageId,
            Acknowledgment acknowledgment) {

        if (
                messageRepository.findById(messageId)
                        .isPresent()
        ) {
            logRepeatedMessage();
            return;
        }

        try {

            assetsService.initAssetsCompensation(
                    command.getProductId()
            );

            messageRepository.save(
                    new Message(messageId, Instant.now(), MessageType.COMMAND)
            );

            acknowledgment.acknowledge();

        } catch (Exception e) {

            log.error("Occurred exception when processing compensation saga product assets message. " +
                    "Acknowledgement was not updated", e
            );

        }


    }

    private void logRepeatedMessage() {
        log.debug("Caught a repeated message!");
    }

    @KafkaHandler
    public void handle(Object ignored) {
        log.trace("Received command that is not published for assets service");
    }

}
