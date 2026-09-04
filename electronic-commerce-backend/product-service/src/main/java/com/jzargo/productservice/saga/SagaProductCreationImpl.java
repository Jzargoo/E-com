package com.jzargo.productservice.saga;

import com.jzargo.productservice.config.ApplicationPropertyStorage;
import com.jzargo.productservice.entity.SagaProductEntity;
import com.jzargo.productservice.entity.SagaStatus;
import com.jzargo.productservice.entity.SagaStep;
import com.jzargo.productservice.exception.CategoryNotFoundException;
import com.jzargo.productservice.exception.ProductNotFoundException;
import com.jzargo.productservice.exception.SagaEntityNotFoundException;
import com.jzargo.productservice.model.CreateAndUpdateProductDetails;
import com.jzargo.productservice.repository.SagaProductCreationRepository;
import com.jzargo.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SagaProductCreationImpl implements SagaProductCreation {

    private final ProductService productService;
    private final SagaProductCreationRepository sagaProductCreationRepository;
    private final ApplicationPropertyStorage applicationPropertyStorage;

    @Override
    @Transactional
    public void initiateProductCreation(CreateAndUpdateProductDetails details) throws CategoryNotFoundException {

        log.info("Details: {}", details);

        Long productId = productService.createProduct(details);

        Long expirationTimeInSeconds = applicationPropertyStorage.getSaga().getExpirationTimeInSeconds();

        SagaProductEntity sagaEntity= SagaProductEntity.builder()
                .id(productId)
                .step(SagaStep.PENDING_INVENTORY)
                .status(SagaStatus.PROCESSING)
                .shopId(details.getShopId())
                .price(
                        details.getPrice()
                )
                .expirationDate(
                        LocalDateTime.now().plusSeconds(expirationTimeInSeconds)
                )
                .build();

        sagaProductCreationRepository.save(sagaEntity);
    }

    @Override
    @Transactional
    public void createdInventoryEntry(Long productId) throws SagaEntityNotFoundException {
        updateStep(productId, Optional.empty(), SagaStep.PENDING_ASSETS, SagaStep.PENDING_INVENTORY);
    }

    @Override
    @Transactional
    public void createdPriceEntry(Long productId) throws SagaEntityNotFoundException {
        updateStep(productId, Optional.empty(),SagaStep.PENDING_PRICE, SagaStep.PENDING_PRICE);
    }

    @Override
    @Transactional
    public void createdAssetsEntry(Long productId) throws SagaEntityNotFoundException {
        updateStep(productId, Optional.empty(),SagaStep.FINISHED, SagaStep.PENDING_ASSETS);

        SagaProductEntity sagaProductEntity = sagaProductCreationRepository
                .findById(productId)
                .orElseThrow(SagaEntityNotFoundException::new);

        sagaProductEntity.setStatus(
                SagaStatus.COMPLETED
        );

        sagaProductCreationRepository.save(sagaProductEntity);

    }

    @Override
    @Transactional
    public void compensatedAssetsEntry(Long productId, String errorMessage) throws SagaEntityNotFoundException {
        updateStep(productId, Optional.ofNullable(errorMessage),SagaStep.PENDING_ASSETS, SagaStep.COMPENSATE_PRICE);
    }



    @Override
    @Transactional
    public void compensatedInventoryEntry(Long productId,  String errorMessage) throws SagaEntityNotFoundException {
        updateStep(productId, Optional.ofNullable(errorMessage), SagaStep.COMPENSATE_PRODUCT, SagaStep.PENDING_INVENTORY, SagaStep.COMPENSATE_INVENTORY);
    }

    @Override
    @Transactional
    public void compensatedPriceEntry(Long productId,  String errorMessage) throws SagaEntityNotFoundException {
        updateStep(productId, Optional.ofNullable(errorMessage), SagaStep.COMPENSATE_INVENTORY, SagaStep.PENDING_PRICE);
    }

    @Override
    @Transactional
    public void compensateProductEntry(Long productId) throws SagaEntityNotFoundException, ProductNotFoundException {
        SagaProductEntity sagaProductEntity = sagaProductCreationRepository
                .findById(productId)
                .orElseThrow(SagaEntityNotFoundException::new);

        if(!SagaStep.COMPENSATE_PRODUCT.equals(sagaProductEntity.getStep())) {

            warn_unexpectedStep(productId, sagaProductEntity.getStep(), SagaStep.FAILED);

            throw new SagaEntityNotFoundException(); //TODO: throw custom exception
        }

        productService.deleteProduct(productId);

        sagaProductEntity.setStep(SagaStep.FAILED);

        sagaProductCreationRepository.save(sagaProductEntity);
    }


    private void updateStep(Long productId, Optional<String> errorMessage , SagaStep newSagaStep, SagaStep... expected) throws SagaEntityNotFoundException {
        SagaProductEntity sagaProductEntity = sagaProductCreationRepository
                .findById(productId)
                .orElseThrow(SagaEntityNotFoundException::new);

        log.debug("Updating saga step for productId: {}. " +
                "Current step: {}, New step: {} ",
                productId, sagaProductEntity.getStep(), newSagaStep);

        if (Arrays.stream(expected).noneMatch(step -> step.equals(sagaProductEntity.getStep()))) {
            warn_unexpectedStep(productId, sagaProductEntity.getStep(), newSagaStep);

            throw new SagaEntityNotFoundException();
        }

        errorMessage.ifPresent(sagaProductEntity::setErrorMessage);

        sagaProductEntity.setStep(newSagaStep);

        sagaProductEntity.setPrice(
                sagaProductEntity.getPrice()
        ); // to include a price into after debezium

        sagaProductCreationRepository.save(sagaProductEntity);
    }

    private void warn_unexpectedStep(Long productId, SagaStep currentStep, SagaStep newStep) {
        log.warn("Unexpected saga step for productId: {}. " +
                        "Current step: {}, New step: {}",
                productId, currentStep, newStep);
    }
}