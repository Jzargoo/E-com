package com.jzargo.inventory.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {"kafka.streams.enabled=false", "kafka.enabled=true"})
@EmbeddedKafka(
		topics = "${kafka.topics.productCreateSaga.name}",
		bootstrapServersProperty = "${spring.kafka.bootstrap-servers}"
)
@ActiveProfiles("test")
class InventoryServiceApplicationTests {

	@Test
	void contextLoads() {

	}

}
