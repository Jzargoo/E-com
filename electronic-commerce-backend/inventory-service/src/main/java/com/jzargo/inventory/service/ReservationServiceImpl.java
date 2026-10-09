package com.jzargo.inventory.service;

import com.jzargo.inventory.dto.AddReservationRequest;
import com.jzargo.inventory.entity.Inventory;
import com.jzargo.inventory.entity.Reservation;
import com.jzargo.inventory.entity.ReservationStatus;
import com.jzargo.inventory.exception.InventoryNotFoundException;
import com.jzargo.inventory.repository.InventoryRepository;
import com.jzargo.inventory.repository.ReservationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class ReservationServiceImpl implements ReservationService {

    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;

    public ReservationServiceImpl(InventoryRepository inventoryRepository, ReservationRepository reservationRepository) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
    }

    @Override
    public void addReservation(AddReservationRequest req) throws InventoryNotFoundException {

        Inventory inventory = inventoryRepository.findByProductId(
                req.getProductId()
        ).orElseThrow(
                () -> new InventoryNotFoundException("Not found for id: " + req.productId)
        );

        Reservation reservation = Reservation.builder()
                .orderId(req.getOrderId())
                .customerId(req.getCustomerId())
                .build();

        reservation.setInventory(inventory);

        reservationRepository.save(reservation);

    }

    @Override
    public void applyReservation(Long orderId) {
        Reservation reservation = reservationRepository.findById(orderId)
                .orElseThrow();

        reservation.setStatus(ReservationStatus.APPLIED);

        reservationRepository.save(reservation);
    }

}
