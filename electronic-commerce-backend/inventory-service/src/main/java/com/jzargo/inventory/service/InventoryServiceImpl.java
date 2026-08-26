package com.jzargo.inventory.service;

import com.jzargo.inventory.dto.ChangeStockDto;
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
    public void createInventory(Long productId, Integer shopId) {

        Inventory inventory = Inventory.builder()
                .productId(productId)
                .shopId(shopId)
                .build();

        inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public ChangeStockDto addStock(ChangeStockDto request, Integer shopId) throws InventoryNotFoundException {

        Inventory inventory = inventoryRepository
                .findByProductIdAndShopId(request.getProductId(), shopId)
                .orElseThrow(
                        () -> new InventoryNotFoundException(
                                "Inventory not found with id: " + request.getProductId())
                );

        inventory.addQuantity(
                request.getQuantity()
        );

        request.setQuantity(
                inventory.getQuantity()
        );

        inventoryRepository.save(inventory);

        return request;

    }

    @Override
    public ChangeStockDto removeStock(ChangeStockDto request, Integer shopId) throws InventoryNotFoundException {

        Inventory inventory = inventoryRepository
                .findByProductIdAndShopId(request.getProductId(), shopId)
                .orElseThrow(
                        () -> new InventoryNotFoundException(
                                "Inventory not found with id: " + request.getProductId())
                );

        inventory.removeQuantity(
                request.getQuantity()
        );

        request.setQuantity(
                inventory.getQuantity()
        );

        inventoryRepository.save(inventory);

        return request;
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