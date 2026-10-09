package com.jzargo.inventory.unit;

import com.jzargo.core.command.createProductSaga.CompensateInventoryCommand;
import com.jzargo.core.command.createProductSaga.InventoryCommand;
import com.jzargo.inventory.entity.Message;
import com.jzargo.inventory.exception.InventoryAlreadyExistException;
import com.jzargo.inventory.exception.InventoryHasReservationException;
import com.jzargo.inventory.listener.KafkaSagaListener;
import com.jzargo.inventory.repository.MessageRepository;
import com.jzargo.inventory.service.InventoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InventoryCommandHandlerTest {
    private final Long productId = 2L;
    private final Integer shopId = 1;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private KafkaSagaListener inventoryCommandHandler;

    @Test
    @DisplayName("should handle inventory command successfully")
    public void test_handleInventoryCommand_successful() throws InventoryAlreadyExistException {

        var messageId = UUID.randomUUID().toString();

        var command = new InventoryCommand(productId, shopId);

        when(
                messageRepository.existsById(messageId)
        ).thenReturn(false);

        inventoryCommandHandler.handleInventoryCommand(
                command,
                messageId,
                acknowledgment
        );

        verify(
                inventoryService,
                times(1)
        ).createInventory(productId, shopId);

        ArgumentCaptor<Message> captor =
                ArgumentCaptor.forClass(Message.class);

        verify(
                messageRepository,
                times(1)
        ).save(captor.capture());

        assertNotNull(captor.getValue());

        verify(
                acknowledgment,
                times(1)
        ).acknowledge();
    }

    @Test
    @DisplayName("should not process repeated inventory command")
    public void test_handleInventoryCommand_repeatedMessage() throws InventoryAlreadyExistException {

        var messageId = UUID.randomUUID().toString();

        var command = new InventoryCommand(productId, shopId);

        when(
                messageRepository.existsById(messageId)
        ).thenReturn(true);

        inventoryCommandHandler.handleInventoryCommand(
                command,
                messageId,
                acknowledgment
        );

        verify(
                inventoryService,
                never()
        ).createInventory(any(), any());

        verify(
                messageRepository,
                never()
        ).save(any(Message.class));

        verify(
                acknowledgment,
                never()
        ).acknowledge();
    }

    @Test
    @DisplayName("should not acknowledge inventory command on failure")
    public void test_handleInventoryCommand_failure() throws InventoryAlreadyExistException {

        var messageId = UUID.randomUUID().toString();

        var command = new InventoryCommand(productId, shopId);

        when(
                messageRepository.existsById(messageId)
        ).thenReturn(false);

        doThrow(new RuntimeException("Inventory creation failed"))
                .when(inventoryService)
                .createInventory(productId, shopId);

        assertDoesNotThrow(() ->
                inventoryCommandHandler.handleInventoryCommand(
                        command,
                        messageId,
                        acknowledgment
                )
        );

        verify(
                messageRepository,
                never()
        ).save(any(Message.class));

        verify(
                acknowledgment,
                never()
        ).acknowledge();
    }

    @Test
    @DisplayName("should compensate inventory command successfully")
    public void test_handleCompensationInventoryCommand_successful()
            throws InventoryHasReservationException {

        var messageId = UUID.randomUUID().toString();

        var command = new CompensateInventoryCommand(productId, shopId);

        when(
                messageRepository.existsById(messageId)
        ).thenReturn(false);

        inventoryCommandHandler.handleCompensationInventoryCommand(
                command,
                messageId,
                acknowledgment
        );

        verify(
                inventoryService,
                times(1)
        ).deleteInventory(productId, shopId);

        ArgumentCaptor<Message> captor =
                ArgumentCaptor.forClass(Message.class);

        verify(
                messageRepository,
                times(1)
        ).save(captor.capture());

        assertNotNull(captor.getValue());

        verify(
                acknowledgment,
                times(1)
        ).acknowledge();
    }

    @Test
    @DisplayName("should not process repeated compensation command")
    public void test_handleCompensationInventoryCommand_repeatedMessage()
            throws InventoryHasReservationException {

        var messageId = UUID.randomUUID().toString();

        var command = new CompensateInventoryCommand(productId, shopId);

        when(
                messageRepository.existsById(messageId)
        ).thenReturn(true);

        inventoryCommandHandler.handleCompensationInventoryCommand(
                command,
                messageId,
                acknowledgment
        );

        verify(
                inventoryService,
                never()
        ).deleteInventory(any(), any());

        verify(
                messageRepository,
                never()
        ).save(any(Message.class));

        verify(
                acknowledgment,
                never()
        ).acknowledge();
    }
}