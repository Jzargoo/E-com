package com.jzargo.productservice.saga;

import com.jzargo.productservice.model.CreateAndUpdateProductDetails;

public interface SagaProductCreationManager {

    void startSaga (CreateAndUpdateProductDetails details);

    void notifyInventoryService(Long productId);

    void notifyPricingService(Long productId, Double stockPrice);

    void notifyMediaService(Long productId);

    void notifyInventoryServiceCompensation(Long productId);

    void notifyPricingServiceCompensation(Long productId);

    void notifyMediaServiceCompensation(Long productId);

}
