package com.jzargo.inventory.api;

import com.jzargo.inventory.GlobalLogger;
import com.jzargo.inventory.dto.ChangeStockDto;
import com.jzargo.inventory.exception.InventoryNotFoundException;
import com.jzargo.inventory.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }


    @PutMapping("/add/{productId}")
    public ResponseEntity<ChangeStockDto> addStock(
            @RequestBody ChangeStockDto changeStockDto,

            @AuthenticationPrincipal Jwt jwt

    ) throws InventoryNotFoundException {

        GlobalLogger.logStartingExecution("remove stock in controller");

        Integer shopId = jwt.getClaim("shop-id");

        return ResponseEntity.ok(
                inventoryService.addStock(changeStockDto, shopId)
        );

    }

    @PutMapping("/remove/{productId}")
    public ResponseEntity<ChangeStockDto> removeStock(
            @RequestBody ChangeStockDto changeStockDto,
            @AuthenticationPrincipal Jwt jwt
            ) throws InventoryNotFoundException {

        GlobalLogger.logStartingExecution("remove stock in controller");

        Integer shopId = jwt.getClaim("shop-id");

        return ResponseEntity.ok(
                inventoryService.removeStock(changeStockDto,  shopId)
        );

    }
}