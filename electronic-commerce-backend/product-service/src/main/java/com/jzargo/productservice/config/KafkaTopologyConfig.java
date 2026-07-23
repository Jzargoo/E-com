package com.jzargo.productservice.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jzargo.productservice.helper.DebeziumMessageParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.support.serializer.JacksonJsonSerde;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Slf4j
@EnableKafkaStreams
public class KafkaTopologyConfig {

    private final KafkaPropertyStorage kafkaPropertyStorage;
    private final ObjectMapper objectMapper;

    @Autowired
    @SuppressWarnings("unchecked")
    public void sagaCreateProductKStream(StreamsBuilder builder) {

        String sagaEntitiesDebeziumTopicName =
                kafkaPropertyStorage.getTopics().getSagaEntitiesDebeziumTopicName();

        String sagaProductCreateTopicName =
                kafkaPropertyStorage.getTopics().getProductCreateSaga().getName();


        builder

                .stream(
                        sagaEntitiesDebeziumTopicName,
                        Consumed.with(
                                new Serdes.StringSerde(),
                                new Serdes.StringSerde()
                        )
                )

                .peek(
                        (key, value) -> log.info("Kafka streams started processing a message with key {}", key)
                )

                .mapValues(
                        value -> {
                            try {
                                return objectMapper.readValue(
                                        value, Map.class
                                );
                            } catch (JsonProcessingException e) {
                                log.error(
                                        "The saga create product stream " +
                                        "caught an error related with json processing",e
                                );
                                throw new RuntimeException(e);
                            }
                        }
                )

                .filter(
                        (key, value) -> {
                            var op = DebeziumMessageParser.getOperationByRoot(
                                    (Map<String, Object>) value
                            );

                            return op.equals("c") ||
                                    op.equals("u") ||
                                    op.equals("r");
                        }
                )

                .peek(
                        (key, value) ->
                                log.info(
                                        "Caught a message made by debezium with payload {}",
                                        value
                                )
                )

                .map(
                        (key, value) -> {


                            var after = DebeziumMessageParser.getAfterByRoot(
                                    (Map<String, Object>) value
                            );

                            Number nid = (Number) after.get("id");

                            Long id = nid.longValue();


                            var command = DebeziumMessageParser.getSagaCreateCommandByAfter(after);


                            return KeyValue.pair(id.toString(), command);

                        })

                .to(sagaProductCreateTopicName,
                        Produced.with(
                                new Serdes.StringSerde(),
                                new JacksonJsonSerde<>()
                        )
                );
    }
}
