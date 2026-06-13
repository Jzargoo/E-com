package com.jzargo.productservice.integration;

import com.jzargo.core.command.createProductSaga.InventoryCommand;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import java.util.concurrent.LinkedBlockingQueue;


@KafkaListener(
        topics = "#{@kafkaPropertyStorage.productCreateSaga.name}",
        groupId = "product-service"
)
public class SagaProductCreationConsumer{

    public LinkedBlockingQueue<InventoryCommand> inventoryCommands = new LinkedBlockingQueue<>();
    public LinkedBlockingQueue<String> messageIds = new LinkedBlockingQueue<>();

    @KafkaHandler
    public void handleInventoryCommand(@Payload InventoryCommand inventoryCommand,
                                       @Header("T(com.jzargo.core.KafkaCustomHeaders).IDEMPOTENCY_KEY.getValue()") String messageId
                                       ){

        inventoryCommands.add(inventoryCommand);

        messageIds.add(messageId);
    }
}
