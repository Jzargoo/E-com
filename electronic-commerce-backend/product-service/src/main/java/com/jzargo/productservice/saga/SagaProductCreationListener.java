package com.jzargo.productservice.saga;

import com.jzargo.core.KafkaCustomHeaders;
import com.jzargo.core.command.createProductSaga.*;
import com.jzargo.productservice.entity.Message;
import com.jzargo.productservice.entity.MessageType;
import com.jzargo.productservice.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Transactional
@KafkaListener(
        topics = "#{@kafkaPropertyStorage.topics.productCreateSaga.name}",
        groupId = "#{@kafkaPropertyStorage.groupId}"
)
@Component
public class SagaProductCreationListener {


    private final MessageRepository messageRepository;
    private final SagaProductCreation sagaProductCreation;

    public SagaProductCreationListener(MessageRepository messageRepository, SagaProductCreation sagaProductCreation) {
        this.messageRepository = messageRepository;
        this.sagaProductCreation = sagaProductCreation;
    }

    @KafkaHandler
    @Transactional
    public void handleMediaCommand(
            @Payload MediaCommandResponse command,
            @Header(KafkaCustomHeaders.IDEMPOTENCY_KEY) String messageId
                                       ){
        log.trace("Received media command message from kafka");

        if (messageRepository.findById(messageId).isPresent()) {
            logRepeatedMessage();
            return;
        }

        try{
            sagaProductCreation.initiatedInventoryEntry(command.getProductId());

            messageRepository.save(
                    new Message(messageId, MessageType.COMMAND, Instant.now())
            );
            log.info("Successfully handled inventory message with product id {}", command.getProductId());

        } catch (Exception e) {
            log.error("Unexpected exception occurred!", e);
            messageRepository.deleteById(messageId);
        }

    }
    @KafkaHandler
    @Transactional
    public void handleInventoryCommand(
            @Payload InventoryCommandResponse command,
            @Header(KafkaCustomHeaders.IDEMPOTENCY_KEY) String messageId
    ){
        log.trace("Received inventory command message from kafka");

        if (messageRepository.findById(messageId).isPresent()) {
            logRepeatedMessage();
            return;
        }

        try{
            sagaProductCreation.initiatedInventoryEntry(command.getProductId());

            messageRepository.save(
                    new Message(messageId, MessageType.COMMAND, Instant.now())
            );

            log.info("Successfully handled media message with product id {}", command.getProductId());

        } catch (Exception e) {
            log.error("Unexpected exception occurred!", e);
            messageRepository.deleteById(messageId);
        }
    }

    @KafkaHandler
    @Transactional
    public void handlePriceCommand(
            @Payload PricingCommandResponse command,
            @Header(KafkaCustomHeaders.IDEMPOTENCY_KEY) String messageId
    ){
        log.trace("Received price command message from kafka");

        if (messageRepository.findById(messageId).isPresent()) {
            logRepeatedMessage();
            return;
        }

        try{
            sagaProductCreation.initiatedPriceEntry(command.getProductId());

            messageRepository.save(
                    new Message(messageId, MessageType.COMMAND, Instant.now())
            );

            log.info("Successfully handled price message with product id {}", command.getProductId());

        } catch (Exception e) {
            log.error("Unexpected exception occurred!", e);
            messageRepository.deleteById(messageId);
        }
    }

    @KafkaHandler
    @Transactional
    public void handleCompensatedMediaCommand(
            @Payload MediaCompensationCommandResponse command,
            @Header(KafkaCustomHeaders.IDEMPOTENCY_KEY) String messageId
    ){
        log.trace("Received media compensation command message from kafka");

        if (messageRepository.findById(messageId).isPresent()) {
            logRepeatedMessage();
            return;
        }

        try{
            sagaProductCreation.compensatedMediaEntry(command.getProductId());

            messageRepository.save(
                    new Message(messageId, MessageType.COMMAND, Instant.now())
            );
            log.info("Successfully handled compensation media command message with product id {}", command.getProductId());

        } catch (Exception e) {
            log.error("Unexpected exception occurred!", e);
            messageRepository.deleteById(messageId);
        }

    }
    @KafkaHandler
    @Transactional
    public void handleCompensationInventoryCommand(
            @Payload InventoryCompensationCommandResponse command,
            @Header(KafkaCustomHeaders.IDEMPOTENCY_KEY) String messageId
    ){
        log.trace("Received compensation inventory command message from kafka");

        if (messageRepository.findById(messageId).isPresent()) {
            logRepeatedMessage();
            return;
        }

        try{
            sagaProductCreation.compensatedInventoryEntry(command.getProductId());

            messageRepository.save(
                    new Message(messageId, MessageType.COMMAND, Instant.now())
            );

            log.info("Successfully handled compensation inventory message with product id {}", command.getProductId());

        } catch (Exception e) {
            log.error("Unexpected exception occurred!", e);
            messageRepository.deleteById(messageId);
        }
    }

    @KafkaHandler
    @Transactional
    public void handleCompensationPriceCommand(
            @Payload PricingCompensationCommandResponse command,
            @Header(KafkaCustomHeaders.IDEMPOTENCY_KEY) String messageId
    ){
        log.trace("Received compensation price command message from kafka");

        if (messageRepository.findById(messageId).isPresent()) {
            logRepeatedMessage();
            return;
        }

        try{
            sagaProductCreation.compensatedPriceEntry(command.getProductId());

            messageRepository.save(
                    new Message(messageId, MessageType.COMMAND, Instant.now())
            );

            log.info("Successfully handled compensation price message with product id {}", command.getProductId());

        } catch (Exception e) {
            log.error("Unexpected exception occurred!", e);
            messageRepository.deleteById(messageId);
        }
    }

    private void logRepeatedMessage() {
        log.debug("find repeated message");
    }
}
