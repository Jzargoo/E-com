package com.jzargo.productservice.saga;

import com.jzargo.productservice.exception.CategoryNotFoundException;
import com.jzargo.productservice.exception.ProductNotFoundException;
import com.jzargo.productservice.exception.SagaEntityNotFoundException;
import com.jzargo.productservice.model.CreateAndUpdateProductDetails;

// Saga will be consistent otherwise compensation will be really expensive
/*
    SagaProductCreation is the interface that defines the steps of the saga for product creation.
    It has methods for initiating product creation, inventory entry, and price entry,
    as well as methods for compensating those steps in case of failure.
    Each method throws a specific exception if the entity is not found or if the category is not found.

    SagaProductCreation.initiateProductCreation - a method that tries to create a new product.
        It creates product and create sagaProductEntity for a specific product

    SagaProductCreation.initiatedInventoryEntry - a method that updates sagaProductEntity.
        It is called when the inventory service has successfully created an inventory entry for the product.

    SagaProductCreation.initiatedPriceEntry - a method that updates sagaProductEntity.
        It is called when the price service has successfully created a price entry for the product.

    SagaProductCreation.compensatedInventoryEntry - a method that updates sagaProductEntity.
        It is called when the inventory service has failed to create an inventory entry for the product,
        and the saga needs to compensate by rolling back the inventory entry.

    SagaProductCreation.compensatedPriceEntry - a method that updates sagaProductEntity.
        It is called when the price service has failed to create a price entry for the product,
        and the saga needs to compensate by rolling back the price entry.

    SagaProductCreation.compensatedProductEntry - a method that delete product and update sagaProductEntity.
        It is called when the product service has failed to create a product entry for the product,
        and the saga needs to compensate by rolling back the product entry.
*/
public interface SagaProductCreation {

    void initiateProductCreation(CreateAndUpdateProductDetails details) throws CategoryNotFoundException; // 1 STEP

    void createdInventoryEntry(Long productId) throws SagaEntityNotFoundException; // 2 STEP

    void createdPriceEntry(Long productId) throws SagaEntityNotFoundException; // 3 STEP


    void compensatedInventoryEntry(Long productId) throws SagaEntityNotFoundException;

    void compensatedPriceEntry(Long productId) throws SagaEntityNotFoundException;

    void compensateProductEntry(Long productId) throws SagaEntityNotFoundException, ProductNotFoundException;
}