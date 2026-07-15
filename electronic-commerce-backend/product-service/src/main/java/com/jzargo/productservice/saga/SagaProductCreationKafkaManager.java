package com.jzargo.productservice.saga;

import com.jzargo.core.KafkaCustomHeaders;
import com.jzargo.core.command.createProductSaga.*;
import com.jzargo.productservice.config.KafkaPropertyStorage;
import com.jzargo.productservice.exception.CategoryNotFoundException;
import com.jzargo.productservice.model.CreateAndUpdateProductDetails;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
public class SagaProductCreationKafkaManager implements SagaProductCreationManager{

    private final SagaProductCreation sagaProductCreation;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaPropertyStorage kafkaPropertyStorage;

    public SagaProductCreationKafkaManager(
            SagaProductCreation sagaProductCreation,
            KafkaTemplate<String, Object> kafkaTemplate,
            KafkaPropertyStorage kafkaPropertyStorage
    ) {
        this.sagaProductCreation = sagaProductCreation;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaPropertyStorage = kafkaPropertyStorage;
    }

    @Override
    public void startSaga(CreateAndUpdateProductDetails details) throws CategoryNotFoundException {
        sagaProductCreation.initiateProductCreation(details);
    }

    @Override
    public void notifyInventoryService(Long productId) {
        kafkaTemplate.send(
                createRecord(
                        new InventoryCommand(productId),
                        productId.toString()
                )
        );
        log.info("Sent successfully notify inventory command into kafka topic");
    }

    @Override
    public void notifyPricingService(Long productId, Double stockPrice) {
        kafkaTemplate.send(
                createRecord(
                        new PricingCommand(productId, stockPrice),
                        productId.toString()
                )
        );
    }

    @Override
    public void notifyInventoryServiceCompensation(Long productId) {
        kafkaTemplate.send(
                createRecord(
                        new CompensateInventoryCommand(productId),
                        productId.toString()
                )
        );
    }

    @Override
    public void notifyPricingServiceCompensation(Long productId) {
        kafkaTemplate.send(
                createRecord(
                        new CompensatePricingCommand(productId),
                        productId.toString()
                )
        );
    }

    @Override
    public void notifyProductServiceCompensation(Long productId) {
        kafkaTemplate.send(
                createRecord(
                        new CompensateProductCommand(productId),
                        productId.toString()
                )
        );
    }


    private <T> ProducerRecord<String, T> createRecord(T body, String productId) {
        var record = new ProducerRecord<>(
                kafkaPropertyStorage.getTopics()
                        .getProductCreateSaga().getName(),
                productId,
                body
        );

        var id = UUID.randomUUID().toString();

        record.headers().add(
                KafkaCustomHeaders.IDEMPOTENCY_KEY, id.getBytes(StandardCharsets.UTF_8)
        );

        record.headers().add(
                KafkaCustomHeaders.SAGA_ID_KEY, productId.getBytes(StandardCharsets.UTF_8)
        );

        return record;
    }
}