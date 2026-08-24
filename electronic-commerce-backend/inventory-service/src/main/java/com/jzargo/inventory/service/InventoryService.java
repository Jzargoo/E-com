package com.jzargo.inventory.service;

import com.jzargo.inventory.dto.ChangeStockRequest;
import com.jzargo.inventory.exception.InventoryHasReservationException;
import com.jzargo.inventory.exception.InventoryNotFoundException;

public interface InventoryService {
    void createInventory(Long productId);

    void addStock(ChangeStockRequest request) throws InventoryNotFoundException;

    void deleteInventory(Long productId) throws  InventoryHasReservationException;
}
