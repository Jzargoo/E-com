package com.jzargo.productservice.saga;

import com.jzargo.productservice.entity.SagaProductEntity;
import com.jzargo.productservice.entity.SagaStep;
import com.jzargo.productservice.exception.SagaEntityNotFoundException;
import com.jzargo.productservice.model.CreateAndUpdateProductDetails;
import com.jzargo.productservice.repository.SagaProductCreationRepository;
import com.jzargo.productservice.service.ProductServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SagaProductCreationImpl implements SagaProductCreation {

    private final ProductServiceImpl productServiceImpl;
    private final SagaProductCreationRepository sagaProductCreationRepository;

    @Override
    @Transactional
    public void initiatedProductCreation(CreateAndUpdateProductDetails details) {
        Long productId = productServiceImpl.createProduct(details);

        SagaProductEntity sagaEntity= SagaProductEntity.builder()
                .id(productId)
                .step(SagaStep.PENDING_MEDIA_APPROVE)
                .build();

        sagaProductCreationRepository.save(sagaEntity);
    }

    @Override
    @Transactional
    public void initiatedInventoryEntry(Long productId) throws SagaEntityNotFoundException {

        SagaProductEntity sagaProductEntity = sagaProductCreationRepository
                .findById(productId)
                .orElseThrow(SagaEntityNotFoundException::new);

        sagaProductEntity.setStep(SagaStep.PENDING_PRICE);

        sagaProductCreationRepository.save(sagaProductEntity);
    }

    @Override
    @Transactional
    public void initiatedPriceEntry(Long productId) throws SagaEntityNotFoundException {

        SagaProductEntity sagaProductEntity = sagaProductCreationRepository
                .findById(productId)
                .orElseThrow(SagaEntityNotFoundException::new);

        sagaProductEntity.setStep(SagaStep.FINISHED);

        sagaProductCreationRepository.save(sagaProductEntity);

    }

    @Override
    @Transactional
    public void initiatedMediaEntry(Long productId) throws SagaEntityNotFoundException {
        SagaProductEntity sagaProductEntity = sagaProductCreationRepository
                .findById(productId)
                .orElseThrow(SagaEntityNotFoundException::new);

        sagaProductEntity.setStep(SagaStep.PENDING_INVENTORY);

        sagaProductCreationRepository.save(sagaProductEntity);
    }

    @Override
    @Transactional
    public void compensatedInventoryEntry(Long productId) throws SagaEntityNotFoundException {
        SagaProductEntity sagaProductEntity = sagaProductCreationRepository
                .findById(productId)
                .orElseThrow(SagaEntityNotFoundException::new);

        sagaProductEntity.setStep(SagaStep.COMPENSATE_MEDIA);

        sagaProductCreationRepository.save(sagaProductEntity);
    }

    @Override
    @Transactional
    public void compensatedPriceEntry(Long productId) throws SagaEntityNotFoundException {
        SagaProductEntity sagaProductEntity = sagaProductCreationRepository
                .findById(productId)
                .orElseThrow(SagaEntityNotFoundException::new);

        sagaProductEntity.setStep(SagaStep.COMPENSATE_INVENTORY);

        sagaProductCreationRepository.save(sagaProductEntity);
    }

    @Override
    @Transactional
    public void compensatedMediaEntry(Long productId) throws SagaEntityNotFoundException {
        SagaProductEntity sagaProductEntity = sagaProductCreationRepository
                .findById(productId)
                .orElseThrow(SagaEntityNotFoundException::new);

        sagaProductEntity.setStep(SagaStep.FAILED);

        sagaProductCreationRepository.save(sagaProductEntity);
    }
}