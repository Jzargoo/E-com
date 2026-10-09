package com.jzargo.inventory.integration;

import com.jzargo.inventory.listener.KafkaSagaListener;
import com.jzargo.inventory.repository.InventoryRepository;
import com.jzargo.inventory.repository.MessageRepository;
import com.jzargo.inventory.repository.ReservationRepository;
import com.jzargo.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionManagerCustomizationAutoConfiguration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {"kafka.streams.enabled=false", "kafka.enabled=true"})
@EmbeddedKafka(
        topics = "${kafka.topics.productCreateSaga.name}",
        bootstrapServersProperty = "${spring.kafka.bootstrap-servers}"
)
@ActiveProfiles("test")
@ImportAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        DataJpaRepositoriesAutoConfiguration.class,
        TransactionManagerCustomizationAutoConfiguration.class,
        TransactionAutoConfiguration.class
})
@ExtendWith({SpringExtension.class, MockitoExtension.class})
public class InventoryCommandHandlerIntegrationTest {

    @MockitoSpyBean
    public KafkaSagaListener kafkaSagaListener;

    @Autowired
    public KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    public MessageRepository messageRepository;

    @MockitoBean
    public ReservationRepository reservationRepository;

    @MockitoBean
    public InventoryRepository inventoryRepository;

    @MockitoBean
    public InventoryService inventoryServices;


    @Test
    public void test_handleInventoryCommand_success(){
        when(
                messageRepository.existsById(any())
        ).thenReturn(false);

        when(
                inventoryService.createInventory
        )
    }

}
