package com.jzargo.inventory.unit;

import com.jzargo.inventory.dto.ChangeStockDto;
import com.jzargo.inventory.entity.Inventory;
import com.jzargo.inventory.exception.InventoryAlreadyExistException;
import com.jzargo.inventory.exception.InventoryHasReservationException;
import com.jzargo.inventory.exception.InventoryNotFoundException;
import com.jzargo.inventory.repository.InventoryRepository;
import com.jzargo.inventory.service.InventoryServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InventoryServiceUnitTest {

    private final Long productId = 1L;
    private final Integer shopId = 2;

    private Inventory inventorySaved = Inventory.builder()
            .productId(productId)
            .shopId(shopId)
            .build();

    @Spy
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    @Test
    @DisplayName("should create inventory")
    public void test_createInventory_successful() throws InventoryAlreadyExistException {

        when(
                inventoryRepository.save(any(Inventory.class))
        ).thenReturn(inventorySaved);

        inventoryService.createInventory(productId, shopId);

        verify(
                inventoryRepository,
                times(1)
        ).save(any(Inventory.class));

    }

    @Test
    @DisplayName("should throws")
    public void test_createInventory_alreadyExists() {

        // ARRANGE
        when(
                inventoryRepository.existsByProductIdAndShopId(any(), any())
        ).thenReturn(true);

        // ACT & ASSERT
        Assertions.assertThrows(
                InventoryAlreadyExistException.class,
                () -> inventoryService.createInventory(any(), any())
        );
    }

    @Test
    @DisplayName("should add stock")
    public void test_addStock_successful() throws InventoryNotFoundException {

        when(
                inventoryRepository.findByProductIdAndShopId(any(), any())
        ).thenReturn(
                Optional.of(inventorySaved)
        );

        var stockCount = 19;


        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);

        when(
                inventoryRepository.save(any())
        ).thenReturn(null);

        inventoryService.addStock(
                new ChangeStockDto(stockCount, productId), shopId
        );

        verify(
                inventoryRepository,
                times(1)
        ).save(captor.capture());

        assertEquals(
                inventorySaved.getQuantity(),
                captor.getValue().getQuantity()
        );

    }

    @Test
    @DisplayName("should throw not found exception")
    public void test_addStock_notFound() {
        when(
                inventoryRepository.findByProductIdAndShopId(any(), any())
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                InventoryNotFoundException.class,
                () -> inventoryService.addStock(
                        new ChangeStockDto(19, productId), shopId
                )
        );
    }

    @Test
    @DisplayName("should remove stock success")
    public void test_removeStock_successful() throws InventoryNotFoundException {

        when(
                inventoryRepository.findByProductIdAndShopId(any(), any())
        ).thenReturn(
                Optional.of(inventorySaved)
        );

        var stockCount = 19;

        inventorySaved.setQuantity(stockCount + 10);

        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);

        when(
                inventoryRepository.save(any())
        ).thenReturn(null);

        inventoryService.removeStock(
                new ChangeStockDto(stockCount, productId), shopId
        );

        verify(
                inventoryRepository,
                times(1)
        ).save(captor.capture());

        assertEquals(
                inventorySaved.getQuantity(),
                captor.getValue().getQuantity()
        );

    }

    @Test
    @DisplayName("should throw not found exception")
    public void test_removeStock_notFound() throws InventoryAlreadyExistException {
        when(
                inventoryRepository.findByProductIdAndShopId(any(), any())
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                InventoryNotFoundException.class,
                () -> inventoryService.addStock(
                        new ChangeStockDto(19, productId), shopId
                )
        );
    }

    @Test
    @DisplayName("should delete inventory")
    public void test_deleteById_successful() throws InventoryHasReservationException {
        when(
                inventoryRepository.existsByProductIdAndShopId(any(), any())
        ).thenReturn(false);

        doNothing().when(
                inventoryRepository
        ).deleteById(productId);

        inventoryService.deleteInventory(productId, shopId);
    }

}