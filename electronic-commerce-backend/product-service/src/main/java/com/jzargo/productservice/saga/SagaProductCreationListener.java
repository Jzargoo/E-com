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
import java.util.Optional;

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
                sagaProductCreation.createdInventoryEntry(command.getProductId());

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
                sagaProductCreation.createdPriceEntry(command.getProductId());

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
                var errorMessage = Optional.ofNullable(command.getErrorMessage());

                sagaProductCreation.compensatedInventoryEntry(command.getProductId(), errorMessage);

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
                var errorMessage = Optional.ofNullable(command.getErrorMessage());

                sagaProductCreation.compensatedPriceEntry(
                        command.getProductId(),
                        errorMessage
                );

                messageRepository.save(
                        new Message(messageId, MessageType.COMMAND, Instant.now())
                );

                log.info("Successfully handled compensation price message with product id {}", command.getProductId());

            } catch (Exception e) {
                log.error("Unexpected exception occurred!", e);
                messageRepository.deleteById(messageId);
            }
        }

        @KafkaHandler
        @Transactional
        public void handleCompensatedProductCommand(
                @Payload MediaCompensationCommandResponse command,
                @Header(KafkaCustomHeaders.IDEMPOTENCY_KEY) String messageId
        ){
            log.trace("Received media compensation command message from kafka");

            if (messageRepository.findById(messageId).isPresent()) {
                logRepeatedMessage();
                return;
            }

            try{
                sagaProductCreation.compensateProductEntry(command.getProductId());

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
        public void handleForeignMessage(Object foreignMessage) {
            log.trace("Received foreign message from kafka! The message is {}", foreignMessage);
        }

        private void logRepeatedMessage() {
            log.debug("found repeated message");
        }
}
