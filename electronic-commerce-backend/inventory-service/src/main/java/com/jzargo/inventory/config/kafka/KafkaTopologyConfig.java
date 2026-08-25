package com.jzargo.inventory.config.kafka;

import com.jzargo.core.KafkaCustomHeaders;
import com.jzargo.core.command.createProductSaga.InventoryCommandResponse;
import com.jzargo.core.command.createProductSaga.InventoryCompensationCommandResponse;
import com.jzargo.core.command.createProductSaga.SagaProductCreationCommand;
import com.jzargo.core.helper.DebeziumParser;
import com.jzargo.inventory.GlobalLogger;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.support.serializer.JacksonJsonSerde;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

@Configuration
@EnableKafkaStreams
@ConditionalOnBooleanProperty("kafka.enabled")
public class KafkaTopologyConfig {

    private final KafkaPropertyStorage kafkaPropertyStorage;
    private final ObjectMapper objectMapper;

    public KafkaTopologyConfig(KafkaPropertyStorage kafkaPropertyStorage, ObjectMapper objectMapper) {
        this.kafkaPropertyStorage = kafkaPropertyStorage;
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    @Autowired
    public void kafkaStream(StreamsBuilder builder) {

        String debeziumInventoryTopic =
                kafkaPropertyStorage.getTopics().getDebeziumInventoryTopic();

        String toTopic =
                kafkaPropertyStorage.getTopics().getProductCreateSaga().getName();

        builder
                .stream(
                        debeziumInventoryTopic,
                        Consumed.with(
                                Serdes.String(),
                                Serdes.String()
                        )
                )

                .peek(
                        (key, value) ->
                                GlobalLogger.logStartingExecution("kafka stream for key: " + key)
                )

                .mapValues(
                        value ->
                                (Map<String, Object>) objectMapper.readValue(value, Map.class)
                )

                .filter(
                        (key, root) -> {

                            String op = DebeziumParser.getOperationByRoot(root);

                            return op.equalsIgnoreCase("c") ||  op.equalsIgnoreCase("d");
                        }
                )

                .map(this::determineNewPair)

                .peek(GlobalLogger::logProcessedStreamMessage)
                
                .processValues(
                        () -> new FixedKeyProcessor<String, SagaProductCreationCommand, SagaProductCreationCommand>() {

                            private FixedKeyProcessorContext<String, SagaProductCreationCommand> context;

                            @Override
                            public void init(FixedKeyProcessorContext<String, SagaProductCreationCommand> context) {

                                FixedKeyProcessor.super.init(context);

                                this.context = context;

                            }

                            @Override
                            public void process(FixedKeyRecord<String, SagaProductCreationCommand> record) {

                                var productId = record.value().getProductId();

                                record.headers()
                                        .add(
                                                KafkaCustomHeaders.IDEMPOTENCY_KEY,
                                                UUID.randomUUID().toString().getBytes()
                                        )
                                        .add(
                                                KafkaCustomHeaders.SAGA_ID_KEY,
                                                String.valueOf(productId).getBytes()
                                        );

                                context.forward(record);
                            }

                        }
                )
                    

                .to(
                        toTopic,
                        Produced.with(
                                Serdes.String(),
                                new JacksonJsonSerde<>()
                        )
                );

    }

    private KeyValue<String, SagaProductCreationCommand> determineNewPair(String s, Map<String, Object> root) {

        String operationByRoot = DebeziumParser.getOperationByRoot(root);

        SagaProductCreationCommand payload;

        long id;


        if (operationByRoot.equalsIgnoreCase("c")) {

            Map<String, Object> afterByRoot = DebeziumParser.getAfterByRoot(root);

            var nid = (Number) afterByRoot.get("product_id");

            id = nid.longValue();

            payload = new InventoryCommandResponse(id);

        } else {

            Map<String, Object> beforeByRoot = DebeziumParser.getBeforeByRoot(root);

            var nid =  (Number) beforeByRoot.get("product_id");

            id = nid.longValue();

            payload = new InventoryCompensationCommandResponse(id, null);

        }

        return new KeyValue<>(
                String.valueOf(id),
                payload
        );

    }

}
