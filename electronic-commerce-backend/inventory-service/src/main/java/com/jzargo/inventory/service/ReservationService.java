package com.jzargo.inventory.service;


import com.jzargo.inventory.dto.AddReservationRequest;
import com.jzargo.inventory.exception.InventoryNotFoundException;

public interface ReservationService {

    /**
     * Adds reservation of the product from inventory
     * @param reservation request from a user
     **/
    void addReservation(AddReservationRequest reservation) throws InventoryNotFoundException;

    /**
     * Removes reservation of the product from inventory
     * @param orderId as reservation will be applied if order service pushes event
     **/
    void applyReservation(Long orderId);
}
