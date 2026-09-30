package com.jzargo.productservice.integration;

import com.jzargo.core.KafkaCustomHeaders;
import com.jzargo.core.command.createProductSaga.InventoryCommandResponse;
import com.jzargo.productservice.config.kafka.KafkaPropertyStorage;
import com.jzargo.productservice.exception.SagaEntityNotFoundException;
import com.jzargo.productservice.repository.MessageRepository;
import com.jzargo.productservice.saga.SagaProductCreation;
import com.jzargo.productservice.saga.SagaProductCreationListener;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "kafka.streams.enabled=false")
@ActiveProfiles("test")
@EmbeddedKafka(
        topics = "${kafka.topics.productCreateSaga.name}",
        bootstrapServersProperty = "${spring.kafka.bootstrap-servers}"
)
public class SagaProductCreationKafkaIntegrationTest {
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    private KafkaPropertyStorage kafkaPropertyStorage;

    @MockitoSpyBean
    SagaProductCreationListener sagaProductCreationListener;

    @MockitoBean
    private SagaProductCreation sagaProductCreation;
    @MockitoBean
    private MessageRepository messageRepository;

    @Test
    @DisplayName("receive and handle inventory command response")
    public void test_handleInventoryCommandResponse() throws SagaEntityNotFoundException {

        String topicName = kafkaPropertyStorage.getTopics().getProductCreateSaga().getName();

        long productId = 1L;

        String uuid = UUID.randomUUID().toString();

        var sentValue = new InventoryCommandResponse(productId);

        Mockito.when(
                messageRepository.existsById(Mockito.anyString())
        ).thenReturn(false);

        doNothing().when(
                sagaProductCreation
        ).createdInventoryEntry(any());

        sendRecord(topicName, productId, sentValue, uuid);

        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() ->
                    verify(
                            sagaProductCreationListener
                    ).handleInventoryCommand(any(), any())
                );

        ArgumentCaptor<InventoryCommandResponse> valueCaptor = ArgumentCaptor.forClass(InventoryCommandResponse.class);
        ArgumentCaptor<String> messageIdCaptor = ArgumentCaptor.forClass(String.class);


        verify(
                sagaProductCreationListener
        ).handleInventoryCommand(valueCaptor.capture(), messageIdCaptor.capture());

        String messageId = messageIdCaptor.getValue();

        InventoryCommandResponse value = valueCaptor.getValue();

        assertEquals(uuid, messageId);

        assertEquals(productId, value.getProductId());
    }

    private void sendRecord(String topicName, long productId, InventoryCommandResponse sentValue, String uuid) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(
                topicName, Long.toString(productId),
                sentValue
        );

        record.headers()
                .add(
                        KafkaCustomHeaders.IDEMPOTENCY_KEY, uuid.getBytes()
                );

        kafkaTemplate.send(record);
    }

    @Test
    public void test_handlingMessageTwice(){

        // ARRANGE
        String topicName = kafkaPropertyStorage.getTopics().getProductCreateSaga().getName();

        long productId = 1L;

        String uuid = UUID.randomUUID().toString();

        var sentValue = new InventoryCommandResponse(productId);

        Mockito.when(
                messageRepository.existsById(Mockito.anyString())
        ).thenReturn(true);

        // ACT
        sendRecord(topicName, productId, sentValue, uuid);

        Awaitility.await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        verify(sagaProductCreationListener)
                                .handleInventoryCommand(
                                        any(), any()
                                )
                );

        verifyNoInteractions(sagaProductCreation);
    }

}
