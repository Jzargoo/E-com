package com.jzargo.productservice.integration;

import org.springframework.context.annotation.Bean;

@org.springframework.boot.test.context.TestConfiguration
public class TestConfiguration {
    @Bean
    public SagaProductCreationConsumer testListener(){
        return new SagaProductCreationConsumer();
    }
}
