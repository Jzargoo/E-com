package com.jzargo.inventory.service;

import com.jzargo.inventory.dto.ChangeStockRequest;
import com.jzargo.inventory.exception.InventoryHasReservationException;
import com.jzargo.inventory.exception.InventoryNotFoundException;
import com.jzargo.inventory.repository.InventoryRepository;
import com.jzargo.inventory.entity.Inventory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService{

    private final InventoryRepository inventoryRepository;

    public InventoryServiceImpl(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    @Transactional
    public void createInventory(Long productId) {

        Inventory inventory = Inventory.builder()
                .productId(productId)
                .build();

        inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public void addStock(ChangeStockRequest request) throws InventoryNotFoundException {

        Inventory inventory = inventoryRepository.findById(request.getProductId())
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found with id: " + request.getProductId()));

        inventory.updateQuantity(
                request.getQuantity()
        );

        inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public void deleteInventory(Long productId) throws InventoryHasReservationException {

        try {

            inventoryRepository.deleteById(productId);

        } catch (Exception e){
            throw new InventoryHasReservationException("Could not delete the inventory with id" + productId);
        }

    }

}