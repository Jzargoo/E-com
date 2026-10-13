package com.jzargo.inventory.integration;

import com.jzargo.core.KafkaCustomHeaders;
import com.jzargo.core.command.createProductSaga.InventoryCommand;
import com.jzargo.inventory.config.kafka.KafkaPropertyStorage;
import com.jzargo.inventory.exception.InventoryAlreadyExistException;
import com.jzargo.inventory.listener.KafkaSagaListener;
import com.jzargo.inventory.repository.InventoryRepository;
import com.jzargo.inventory.repository.MessageRepository;
import com.jzargo.inventory.repository.ReservationRepository;
import com.jzargo.inventory.service.InventoryService;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionManagerCustomizationAutoConfiguration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {"kafka.streams.enabled=false", "kafka.enabled=true"})
@EmbeddedKafka(
        topics = "${kafka.topics.productCreateSaga.name}",
        bootstrapServersProperty = "${spring.kafka.bootstrap-servers}"
)
@ActiveProfiles("test")
@ImportAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        DataJpaRepositoriesAutoConfiguration.class,
        TransactionManagerCustomizationAutoConfiguration.class,
        TransactionAutoConfiguration.class
})
public class InventoryCommandHandlerIntegrationTest {

    @MockitoSpyBean
    public KafkaSagaListener kafkaSagaListener;

    @Autowired
    public KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    public MessageRepository messageRepository;

    @MockitoBean
    public ReservationRepository reservationRepository;

    @MockitoBean
    public InventoryRepository inventoryRepository;

    @MockitoBean
    public InventoryService inventoryService;

    @Autowired
    private KafkaPropertyStorage kafkaPropertyStorage;

    @Test
    public void test_handleInventoryCommand_success() throws InventoryAlreadyExistException {

        when(
                messageRepository.existsById(any())
        ).thenReturn(false);

        doNothing()
                .when(inventoryService)
                .createInventory(any(), any());

        when(
                messageRepository.save(any())
        ).thenReturn(null);

        Long id = 1L;

        InventoryCommand inventoryCommand = new InventoryCommand(id, 1);

        ProducerRecord<String, Object> inventoryRecord = new ProducerRecord<>(
                kafkaPropertyStorage
                        .getTopics()
                        .getProductCreateSaga()
                        .getName(),
                id.toString(),
                inventoryCommand
        );

        String messageId = UUID.randomUUID().toString();

        inventoryRecord.headers().add(
                KafkaCustomHeaders.IDEMPOTENCY_KEY, messageId.getBytes()
        ).add(
                KafkaCustomHeaders.SAGA_ID_KEY, id.toString().getBytes()
        );

        kafkaTemplate.send(inventoryRecord);

        Awaitility
                .await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> verify(
                                kafkaSagaListener
                        ).handleInventoryCommand(any(), any(), any())
                );

        ArgumentCaptor<InventoryCommand> captorInventory = ArgumentCaptor.forClass(InventoryCommand.class);
        ArgumentCaptor<String> captorMessageId = ArgumentCaptor.forClass(String.class);

        verify(
                kafkaSagaListener,
                times(1)
        ).handleInventoryCommand(captorInventory.capture(), captorMessageId.capture(), any());

        String capturedMessageId = captorMessageId.getValue();
        InventoryCommand command = captorInventory.getValue();

        Assertions.assertEquals(messageId, capturedMessageId);

        Assertions.assertEquals(command.getProductId(), inventoryCommand.getProductId());
        Assertions.assertEquals(command.getShopId(), inventoryCommand.getShopId());
    }

    @Test
    public void test_handleInventoryCommand_repeated(){
        doReturn(true)
                .when(messageRepository)
                .existsById(any());

        Long id = 1L;

        InventoryCommand inventoryCommand = new InventoryCommand(id, 1);

        ProducerRecord<String, Object> inventoryRecord = new ProducerRecord<>(
                kafkaPropertyStorage
                        .getTopics()
                        .getProductCreateSaga()
                        .getName(),
                id.toString(),
                inventoryCommand
        );

        String messageId = UUID.randomUUID().toString();

        inventoryRecord.headers().add(
                KafkaCustomHeaders.IDEMPOTENCY_KEY, messageId.getBytes()
        ).add(
                KafkaCustomHeaders.SAGA_ID_KEY, id.toString().getBytes()
        );

        kafkaTemplate.send(inventoryRecord);

        Awaitility
                .await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> verify(kafkaSagaListener)
                                .handleInventoryCommand(any(), any(), any())
                );

        verify(kafkaSagaListener)
                .handleInventoryCommand(any(), any(), any());

        verifyNoInteractions(inventoryService);
    }
}