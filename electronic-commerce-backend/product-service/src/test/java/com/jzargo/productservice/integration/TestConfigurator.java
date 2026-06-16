package com.jzargo.productservice.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;

@TestConfiguration
@EnableKafka
public class TestConfigurator {
    @Bean
    public SagaProductCreationConsumer sagaProductCreationConsumer(){
        return new SagaProductCreationConsumer();
    }
}
