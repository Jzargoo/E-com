package com.jzargo.productAssetsService.config.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jzargo.core.command.createProductSaga.AssetsCompensationResponse;
import com.jzargo.core.command.createProductSaga.AssetsInitializationCommandResponse;
import com.jzargo.core.helper.DebeziumParser;
import com.jzargo.productAssetsService.helper.DebeziumMessageParser;
import com.jzargo.productAssetsService.helper.GlobalLogger;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.support.serializer.JacksonJsonSerde;

import java.util.Map;

@Slf4j
@Configuration
@EnableKafkaStreams
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

        String fromTopic =
                kafkaPropertyStorage.getTopics().getProductAssetsDebeziumTopicName();

        String toTopic =
                kafkaPropertyStorage.getTopics().getProductCreateSaga().getName();


        builder
                .stream(
                        fromTopic,
                        Consumed.with(
                                new Serdes.StringSerde(),
                                new Serdes.StringSerde()
                        )
                )

                .peek(
                        (key, value) ->
                                GlobalLogger.logStartingExecuting("Kafka Stream for debezium product assets with key: " + key)
                )

                .mapValues(
                value -> {

                    try {

                        return (Map<String, Object>) objectMapper.readValue(value, Map.class);

                    } catch (JsonProcessingException e) {

                        GlobalLogger.logException(e, "processing content from debezium topic into map!");

                        throw new RuntimeException(e);

                    }

                })

                .filter(
                        (key, value) -> {
                            String operationByRoot = DebeziumParser.getOperationByRoot(value);

                            return operationByRoot.equalsIgnoreCase("c") ||
                                    operationByRoot.equalsIgnoreCase("d");
                        }
                )

                .map(
                        (key, value) -> {

                            Long productId = DebeziumMessageParser.productIdByProductAsset(
                                    DebeziumMessageParser.getAfterByRoot(value)
                            );

                            if (
                                    DebeziumMessageParser
                                            .getOperationByRoot(value)
                                            .equalsIgnoreCase("c")
                            ) {

                                return KeyValue.pair(
                                        productId.toString(),
                                        new AssetsInitializationCommandResponse(productId)
                                );

                            } else {

                                return KeyValue.pair(
                                        productId.toString(),
                                        new AssetsCompensationResponse(productId, null)
                                );

                            }

                        }
                )

                .to(
                        toTopic,
                        Produced.with(
                                new Serdes.StringSerde(),
                            new JacksonJsonSerde<>()
                        )
                );
    }

}