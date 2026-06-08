package com.jzargo.productservice.saga;

import com.jzargo.productservice.model.CreateAndUpdateProductDetails;

public interface SagaProductCreationManager {

    String startSaga (CreateAndUpdateProductDetails details);

    void notifyInventoryService(Long productId);

    void notifyPricingService(Long productId);

    void notifyMediaService(Long productId);
}
