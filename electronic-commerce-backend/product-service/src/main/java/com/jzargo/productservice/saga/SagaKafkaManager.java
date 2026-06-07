package com.jzargo.productservice.saga;

import com.jzargo.productservice.model.CreateAndUpdateProductDetails;
import org.springframework.stereotype.Component;

@Component
public class SagaKafkaManager implements SagaProductCreationManager{

    private final SagaProductCreation sagaProductCreation;

    public SagaKafkaManager(SagaProductCreation sagaProductCreation) {
        this.sagaProductCreation = sagaProductCreation;
    }

    @Override
    public String startSaga(CreateAndUpdateProductDetails details) {

        sagaProductCreation.initiateProductCreation(details);


        return "start_saga.success";
    }
}
