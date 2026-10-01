package com.jzargo.inventory.service;

import com.jzargo.inventory.dto.ChangeStockDto;
import com.jzargo.inventory.exception.InventoryAlreadyExistException;
import com.jzargo.inventory.exception.InventoryHasReservationException;
import com.jzargo.inventory.exception.InventoryNotFoundException;

public interface InventoryService {
    void createInventory(Long productId, Integer shopId) throws InventoryAlreadyExistException;

    ChangeStockDto addStock(ChangeStockDto request, Integer shopId) throws InventoryNotFoundException;

    ChangeStockDto removeStock(ChangeStockDto request, Integer shopId) throws InventoryNotFoundException;

    void deleteInventory(Long productId, Integer shopId) throws  InventoryHasReservationException;
}
