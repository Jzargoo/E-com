package com.jzargo.inventory.integration;

import com.jzargo.inventory.dto.ChangeStockDto;
import com.jzargo.inventory.entity.Inventory;
import com.jzargo.inventory.entity.Status;
import com.jzargo.inventory.exception.InventoryAlreadyExistException;
import com.jzargo.inventory.exception.InventoryNotFoundException;
import com.jzargo.inventory.repository.InventoryRepository;
import com.jzargo.inventory.service.InventoryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(properties = {"kafka.enabled=false", "kafka.streams.enabled=false"})
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext
public class InventoryServiceIntegrationTest {
    private final Long productId = 1L;
    private final Integer shopId = 2;

    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    @DisplayName("should create inventory")
    public void test_createInventory_successful() throws InventoryAlreadyExistException {

        // ACT
        inventoryService.createInventory(productId, shopId);

        // ASSERT
        Optional<Inventory> inventory = inventoryRepository.findByProductIdAndShopId(productId, shopId);

        assertFalse(inventory.isEmpty());

        assertEquals(0, inventory.get().getQuantity());

    }

    @Test
    @DisplayName("should add stock")
    public void test_addStock_successful() throws InventoryNotFoundException {

        // ARRANGE
        inventoryRepository.save(
                Inventory.builder()
                        .shopId(shopId)
                        .productId(productId)
                        .build()
        );

        var count = 10;

        // ACT
        inventoryService.addStock(
                new ChangeStockDto(10, productId), shopId
        );

        // ASSERT
        Optional<Inventory> inventory = inventoryRepository.findByProductIdAndShopId(productId, shopId);

        assertFalse(inventory.isEmpty());

        assertEquals(count, inventory.get().getQuantity());

        assertEquals(Status.POPULATED, inventory.get().getStatus());
    }

    @Test
    @DisplayName("should remove stock")
    public void test_removeStock_successful() throws InventoryNotFoundException {

        // ARRANGE
        var quantity = 10;

        inventoryRepository.save(
                Inventory.builder()
                        .shopId(shopId)
                        .quantity(quantity)
                        .status(Status.POPULATED)
                        .productId(productId)
                        .build()
        );

        var count = 10;

        // ACT
        inventoryService.removeStock(
                new ChangeStockDto(count, productId), shopId
        );

        // ASSERT
        Optional<Inventory> inventory = inventoryRepository.findByProductIdAndShopId(productId, shopId);

        assertFalse(inventory.isEmpty());

        assertEquals(0, inventory.get().getQuantity());

        assertEquals(Status.DEPLETED, inventory.get().getStatus());
    }


}