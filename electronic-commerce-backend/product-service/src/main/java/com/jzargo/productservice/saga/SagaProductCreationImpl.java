package com.jzargo.productservice.saga;

import com.jzargo.productservice.entity.SagaProductEntity;
import com.jzargo.productservice.model.CreateAndUpdateProductDetails;
import com.jzargo.productservice.service.ProductServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SagaProductCreationImpl implements SagaProductCreation {

    private final SagaProductCreationManager manager;
    private final ProductServiceImpl productServiceImpl;

    @Override
    @Transactional
    public void startSaga(CreateAndUpdateProductDetails details) {
        productServiceImpl.createProduct(details);

        SagaProductEntity.builder()

                .build();

    }

    @Override
    public void initiateInventoryEntry() {

    }

    @Override
    public void initiatePriceEntry() {

    }

    @Override
    public void initiateMediaEntry() {

    }

    @Override
    public void compensateInventoryEntry() {

    }

    @Override
    public void compensatePriceEntry() {

    }

    @Override
    public void compensateMediaEntry() {

    }
}