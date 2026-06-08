package com.jzargo.productservice.saga;

import com.jzargo.productservice.model.CreateAndUpdateProductDetails;
import org.springframework.stereotype.Component;

@Component
public class SagaProductCreationKafkaManager implements SagaProductCreationManager{

    private final SagaProductCreation sagaProductCreation;

    public SagaProductCreationKafkaManager(SagaProductCreation sagaProductCreation) {
        this.sagaProductCreation = sagaProductCreation;
    }

    @Override
    public String startSaga(CreateAndUpdateProductDetails details) {

        sagaProductCreation.initiateProductCreation(details);


        return "start_saga.success";
    }
}
