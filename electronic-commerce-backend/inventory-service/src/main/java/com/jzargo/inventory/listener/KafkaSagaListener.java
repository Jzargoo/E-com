package com.jzargo.inventory.listener;

import com.jzargo.core.KafkaCustomHeaders;
import com.jzargo.core.command.createProductSaga.InventoryCommand;
import com.jzargo.inventory.GlobalLogger;
import com.jzargo.inventory.entity.Message;
import com.jzargo.inventory.entity.MessageType;
import com.jzargo.inventory.repository.MessageRepository;
import com.jzargo.inventory.service.InventoryService;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@KafkaListener(
        topics = "#{kafkaPropertyStorage.topics.productCreateSaga.name}",
        groupId = "#{kafkaPropertyStorage.groupId}",
        properties = {"enable.auto.commit=false"},
        containerFactory = "manualListenerContainerFactory"
)
public class KafkaSagaListener {

    private final MessageRepository messageRepository;
    private final InventoryService inventoryService;

    public KafkaSagaListener(MessageRepository messageRepository, InventoryService inventoryService) {
        this.messageRepository = messageRepository;
        this.inventoryService = inventoryService;
    }

    @KafkaHandler
    @Transactional
    public void handleInventoryCommand(
            @Payload InventoryCommand inventoryCommand,
            @Header(KafkaCustomHeaders.IDEMPOTENCY_KEY) String messageId,
            Acknowledgment acknowledgment
    ) {

        if (
                messageRepository.existsById(messageId)
        ) {
            GlobalLogger.logRepeatedMessage(messageId);

            return;
        }

        try {
            inventoryService.createInventory(inventoryCommand.getProductId());

            messageRepository.save(
                    new Message(messageId, MessageType.COMMAND, LocalDateTime.now())
            );

            acknowledgment.acknowledge();


        } catch (Exception e) {
            // Do not set ack

            GlobalLogger.logException(
                    e,
                    "handling inventory command with product id: " + inventoryCommand.getProductId()
            );

            throw e;
        }

    }

}