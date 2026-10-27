package com.jzargo.productservice.eventsListener;

import com.jzargo.core.KafkaCustomHeaders;
import com.jzargo.core.events.DepopulatedInventoryEvent;
import com.jzargo.core.events.PopulatedInventoryEvent;
import com.jzargo.productservice.entity.Message;
import com.jzargo.productservice.entity.MessageType;
import com.jzargo.productservice.repository.MessageRepository;
import com.jzargo.productservice.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
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
@ConditionalOnBooleanProperty("kafka.enabled")
public class KafkaProductEventsListener {

    private final MessageRepository messageRepository;
    private final ProductService productService;

    public KafkaProductEventsListener(MessageRepository messageRepository, ProductService productService) {
        this.messageRepository = messageRepository;
        this.productService = productService;
    }

    @KafkaHandler
    public void handleDepopulatedEvents(
            @Payload DepopulatedInventoryEvent depopulatedInventoryEvent,
            @Header(KafkaCustomHeaders.IDEMPOTENCY_KEY) String messageId
    ) {

        if (messageRepository.existsById(messageId)) {

            logRepeatedMessage(messageId);
            return;

        }

        productService.unavailableProduct(depopulatedInventoryEvent.getProductId());

        messageRepository.save(
                new Message(
                    messageId, MessageType.EVENT, Instant.now()
                )
        );

    }

    private void logRepeatedMessage(String messageId) {
        log.debug("handled repeated message {}", messageId);
    }

    @KafkaHandler
    public void handlePopulatedEvents(
            @Payload PopulatedInventoryEvent populatedInventoryEvent,
            @Header(KafkaCustomHeaders.IDEMPOTENCY_KEY) String messageId
    ) {

        if (messageRepository.existsById(messageId)) {

            logRepeatedMessage(messageId);
            return;

        }

        productService.availableProduct(populatedInventoryEvent.getProductId());

        messageRepository.save(
                new Message(
                        messageId, MessageType.EVENT, Instant.now()
                )
        );

    }

    @KafkaHandler(isDefault = true)
    public void handleUnknown(Object ignored){
        log.trace("handling unknown object! Skipping...");
    }
}