package com.jzargo.productservice.integration;


import com.jzargo.productservice.config.kafka.KafkaTopologyConfig;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.kafka.test.context.EmbeddedKafka;

@IT(properties ="kafka.enabled=false")
@EmbeddedKafka
@ComponentScan(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = KafkaTopologyConfig.class
        )
)
public class ProductMicroserviceIntegrationTest {
    @Test
    public void contextLoads(){
        System.out.println("hello world");
    }
}
