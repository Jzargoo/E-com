package com.jzargo.productservice.saga;

import com.jzargo.productservice.exception.SagaEntityNotFoundException;
import com.jzargo.productservice.model.CreateAndUpdateProductDetails;

// Saga will be consistent otherwise compensation will be really expensive
public interface SagaProductCreation {

    void initiatedProductCreation(CreateAndUpdateProductDetails details); // 1 STEP

    void initiatedInventoryEntry(Long productId) throws SagaEntityNotFoundException; // 2 STEP

    void initiatedPriceEntry(Long productId) throws SagaEntityNotFoundException; // 3 STEP

    void initiatedMediaEntry(Long productId) throws SagaEntityNotFoundException; // 4 STEP


    void compensatedMediaEntry(Long productId) throws SagaEntityNotFoundException;

    void compensatedInventoryEntry(Long productId) throws SagaEntityNotFoundException;

    void compensatedPriceEntry(Long productId) throws SagaEntityNotFoundException;

}