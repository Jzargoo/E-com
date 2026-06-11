package com.jzargo.productservice.saga;

import com.jzargo.core.KafkaCustomHeaders;
import com.jzargo.core.command.createProductSaga.*;
import com.jzargo.productservice.config.KafkaPropertyStorage;
import com.jzargo.productservice.model.CreateAndUpdateProductDetails;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class SagaProductCreationKafkaManager implements SagaProductCreationManager{

    private final SagaProductCreation sagaProductCreation;
    private final KafkaTemplate<Long, Object> kafkaTemplate;
    private final KafkaPropertyStorage kafkaPropertyStorage;

    public SagaProductCreationKafkaManager(
            SagaProductCreation sagaProductCreation,
            KafkaTemplate<Long, Object> kafkaTemplate,
            KafkaPropertyStorage kafkaPropertyStorage
    ) {
        this.sagaProductCreation = sagaProductCreation;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaPropertyStorage = kafkaPropertyStorage;
    }

    @Override
    public void startSaga(CreateAndUpdateProductDetails details) {
        sagaProductCreation.initiatedProductCreation(details);
    }

    @Override
    public void notifyInventoryService(Long productId) {
        kafkaTemplate.send(
                createRecord(
                        new InventoryCommand(productId),
                        productId
                )
        );
    }

    @Override
    public void notifyPricingService(Long productId, Double stockPrice) {
        kafkaTemplate.send(
                createRecord(
                        new PricingCommand(productId, stockPrice),
                        productId
                )
        );
    }

    @Override
    public void notifyMediaService(Long productId) {
        kafkaTemplate.send(
                createRecord(
                        new MediaCommand(productId),
                        productId
                )
        );
    }

    @Override
    public void notifyInventoryServiceCompensation(Long productId) {
        kafkaTemplate.send(
                createRecord(
                        new CompensateInventoryCommand(productId),
                        productId
                )
        );
    }

    @Override
    public void notifyPricingServiceCompensation(Long productId) {
        kafkaTemplate.send(
                createRecord(
                        new CompensatePricingCommand(productId),
                        productId
                )
        );
    }

    @Override
    public void notifyMediaServiceCompensation(Long productId) {
        kafkaTemplate.send(
                createRecord(
                        new CompensateMediaCommand(productId),
                        productId
                )
        );
    }


    private <T> ProducerRecord<Long, T> createRecord(T body, Long productId) {
        var record = new ProducerRecord<>(
                kafkaPropertyStorage.getTopics()
                        .getProductCreateSaga().getName(),
                productId,
                body
        );

        var id = UUID.randomUUID().toString();

        record.headers().add(
                KafkaCustomHeaders.IDEMPOTENCY_KEY.getValue(), id.getBytes(StandardCharsets.UTF_8)
        );

        record.headers().add(
                KafkaCustomHeaders.SAGA_ID_KEY.getValue(), productId.toString().getBytes(StandardCharsets.UTF_8)
        );

        return record;
    }
}
