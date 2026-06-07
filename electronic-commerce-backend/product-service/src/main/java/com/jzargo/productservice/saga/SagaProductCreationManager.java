package com.jzargo.productservice.saga;

import com.jzargo.productservice.model.CreateAndUpdateProductDetails;

public interface SagaProductCreationManager {

    String startSaga (CreateAndUpdateProductDetails details);

}
