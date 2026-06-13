package com.jzargo.productservice.integration;

import com.jzargo.core.command.createProductSaga.InventoryCommand;
import com.jzargo.productservice.config.ApplicationPropertyStorage;
import com.jzargo.productservice.model.CreateAndUpdateProductDetails;
import com.jzargo.productservice.repository.CategoryRepository;
import com.jzargo.productservice.repository.ProductRepository;
import com.jzargo.productservice.repository.SagaProductCreationRepository;
import com.jzargo.productservice.saga.SagaProductCreationImpl;
import com.jzargo.productservice.saga.SagaProductCreationKafkaManager;
import com.jzargo.productservice.service.ImageDriverNative;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


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
@EmbeddedKafka
@IT(properties = "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}")
public class SagaProductCreationKafkaManagerIntegrationTest {

    @Autowired
    public SagaProductCreationKafkaManager sagaProductCreationKafkaManager;
    @Autowired
    public SagaProductCreationConsumer sagaProductCreationConsumer;
    @Autowired
    public KafkaTemplate<Long, Object> kafkaTemplate;

    private long PRODUCT_ID = 12L;

    @MockitoBean
    public SagaProductCreationImpl sagaProductCreation;
    @MockitoBean
    public CategoryRepository categoryRepository;
    @MockitoBean
    public ApplicationPropertyStorage applicationPropertyStorage;
    @MockitoBean
    public ImageDriverNative imageDriverNative;
    @MockitoBean
    public ProductRepository productRepository;
    @MockitoBean
    public SagaProductCreationRepository sagaProductCreationRepository;

    @Test
    public void testNotifyInventoryService_success() throws InterruptedException {
        // Act
        sagaProductCreationKafkaManager.notifyInventoryService(PRODUCT_ID);

        // Assert
        InventoryCommand poll = sagaProductCreationConsumer.inventoryCommands.poll(10, TimeUnit.SECONDS);

        assertNotNull(poll, "Inventory commands queue did not contain any messages");
        assertEquals(poll.getProductId(), PRODUCT_ID);

    }
}
