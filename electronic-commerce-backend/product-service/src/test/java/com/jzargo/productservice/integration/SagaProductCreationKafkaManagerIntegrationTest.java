package com.jzargo.productservice.integration;

import com.jzargo.core.command.createProductSaga.InventoryCommand;
import com.jzargo.productservice.client.MediaServiceClientImpl;
import com.jzargo.productservice.config.ApplicationPropertyStorage;
import com.jzargo.productservice.driver.FallbackMediaDriverNative;
import com.jzargo.productservice.repository.CategoryRepository;
import com.jzargo.productservice.repository.ProductRepository;
import com.jzargo.productservice.repository.SagaProductCreationRepository;
import com.jzargo.productservice.saga.SagaProductCreationImpl;
import com.jzargo.productservice.saga.SagaProductCreationKafkaManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.health.autoconfigure.actuate.endpoint.HealthEndpointAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.CommonsClientAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@EnableAutoConfiguration(exclude = {
        LiquibaseAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        DataSourceInitializationAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class,
        HealthEndpointAutoConfiguration.class,
        WebEndpointAutoConfiguration.class,
        CommonsClientAutoConfiguration.class
})
@DirtiesContext
@SpringBootTest
@Import(SagaProductCreationConsumer.class)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1,controlledShutdown = true, topics = {"${kafka.topics.productEventsTopic.name}"})
public class SagaProductCreationKafkaManagerIntegrationTest {

    @Autowired
    public SagaProductCreationKafkaManager sagaProductCreationKafkaManager;
    @Autowired
    public KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    public SagaProductCreationConsumer sagaProductCreationConsumer;

    private final Long PRODUCT_ID = 12L;

    @Autowired
    private KafkaListenerEndpointRegistry registry;
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @BeforeEach
    void setUp() {
        for (MessageListenerContainer container : registry.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
        }
    }

    @MockitoBean
    public SagaProductCreationImpl sagaProductCreation;
    @MockitoBean
    public CategoryRepository categoryRepository;
    @MockitoBean
    public ApplicationPropertyStorage applicationPropertyStorage;
    @MockitoBean
    public FallbackMediaDriverNative fallbackMediaDriverNative;
    @MockitoBean
    public MediaServiceClientImpl mediaServiceClientImpl;
    @MockitoBean
    public ProductRepository productRepository;
    @MockitoBean
    public SagaProductCreationRepository sagaProductCreationRepository;

    @Test
    public void testNotifyInventoryService_success() throws InterruptedException {
        // Act
        sagaProductCreationKafkaManager.notifyInventoryService(PRODUCT_ID);


        // Assert
        InventoryCommand poll = sagaProductCreationConsumer.inventoryCommands.poll(20, TimeUnit.SECONDS);
        String msId = sagaProductCreationConsumer.messageIds.poll();

        assertNotNull(poll, "The consumer did not find a command");

        assertEquals(PRODUCT_ID.toString(), msId);
    }
}
