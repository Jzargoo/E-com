package com.jzargo.productservice.integration;

import com.jzargo.core.command.createProductSaga.InventoryCommand;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import java.util.concurrent.LinkedBlockingQueue;


@KafkaListener(
        topics = "${kafka.topics.productEventsTopic.name}",
        groupId = "${spring.kafka.consumer.group-id}"
)
public class SagaProductCreationConsumer{

    public LinkedBlockingQueue<InventoryCommand> inventoryCommands = new LinkedBlockingQueue<>();
    public LinkedBlockingQueue<String> messageIds = new LinkedBlockingQueue<>();

    @KafkaHandler
    public void handleInventoryCommand(@Payload InventoryCommand inventoryCommand,
                                       @Header(KafkaHeaders.RECEIVED_KEY) String messageId
                                       ){

        inventoryCommands.add(inventoryCommand);

        messageIds.add(messageId);
    }
}
