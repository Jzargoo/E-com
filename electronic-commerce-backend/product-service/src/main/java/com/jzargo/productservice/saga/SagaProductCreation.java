package com.jzargo.productservice.saga;

import com.jzargo.productservice.model.CreateAndUpdateProductDetails;

// Saga will be consistent otherwise compensation will be really expensive
public interface SagaProductCreation {

    void startSaga(CreateAndUpdateProductDetails details); // 1 STEP

    void initiateInventoryEntry(); // 2 STEP

    void initiatePriceEntry(); // 3 STEP

    void initiateMediaEntry(); // 4 STEP



    void compensateInventoryEntry();

    void compensatePriceEntry();

    void compensateMediaEntry();

}