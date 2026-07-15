package com.jzargo.productservice.saga;

import com.jzargo.productservice.exception.CategoryNotFoundException;
import com.jzargo.productservice.model.CreateAndUpdateProductDetails;

public interface SagaProductCreationManager {

    void startSaga (CreateAndUpdateProductDetails details) throws CategoryNotFoundException;

    void notifyInventoryService(Long productId);

    void notifyPricingService(Long productId, Double stockPrice);

    void notifyInventoryServiceCompensation(Long productId);

    void notifyPricingServiceCompensation(Long productId);

    void notifyProductServiceCompensation(Long productId);

}
