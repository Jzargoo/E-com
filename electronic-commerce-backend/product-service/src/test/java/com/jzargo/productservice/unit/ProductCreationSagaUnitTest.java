package com.jzargo.productservice.unit;

import com.jzargo.productservice.entity.SagaProductEntity;
import com.jzargo.productservice.entity.SagaStep;
import com.jzargo.productservice.exception.CategoryNotFoundException;
import com.jzargo.productservice.exception.SagaEntityNotFoundException;
import com.jzargo.productservice.model.CreateAndUpdateProductDetails;
import com.jzargo.productservice.repository.SagaProductCreationRepository;
import com.jzargo.productservice.saga.SagaProductCreationImpl;
import com.jzargo.productservice.service.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;

import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductCreationSagaUnitTest {

    private static final Long PRODUCT_ID = 12L;

    private SagaProductEntity product;

    private CreateAndUpdateProductDetails details;

    @InjectMocks
    public SagaProductCreationImpl sagaProductCreation;

    @Mock
    public ProductServiceImpl productService;

    @Spy
    public SagaProductCreationRepository repository;

    @BeforeEach
    public void setup(){
        details = new CreateAndUpdateProductDetails(
                0L,
                "product",
                "/products/default",
                BigDecimal.valueOf(100),
                HashMap.newHashMap(1),
                "Good product",
                "Electronics",
                12
        );


        product = SagaProductEntity.builder()
                .id(PRODUCT_ID)
                .build();
    }

    @Test
    public void  test_initiatedProductCreation_success() throws CategoryNotFoundException {
        // Arrange

        when(
                productService.createProduct(details)
        ).thenReturn(PRODUCT_ID);

        // Act
        sagaProductCreation.initiateProductCreation(details);

        // Assert
        verify(
                repository, Mockito.times(1)
        ).save(
                argThat(entity ->
                        entity.getId().equals(PRODUCT_ID) &&
                                entity.getStep().equals(SagaStep.PENDING_INVENTORY)
                )
        );

    }

    @Test
    @DisplayName("Should update step to PENDING_PRICE when inventory is initiated")
    void initiatedInventoryEntry_Success() throws SagaEntityNotFoundException {
        // Arrange
        when(repository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));


        // Act
        sagaProductCreation.createdInventoryEntry(PRODUCT_ID);

        // Assert
        verifyStepUpdate(SagaStep.PENDING_PRICE);
    }

    @Test
    @DisplayName("Should update step to COMPENSATE_PRODUCT when inventory is compensated")
    void compensatedInventoryEntry_Success() throws SagaEntityNotFoundException {
        when(repository.findById(PRODUCT_ID)).thenReturn(Optional.ofNullable(product));

        sagaProductCreation.compensatedInventoryEntry(PRODUCT_ID, Optional.empty());

        verifyStepUpdate(SagaStep.COMPENSATE_PRODUCT);
    }

    @Test
    @DisplayName("Should throw SagaEntityNotFoundException when product is missing")
    void anyMethod_ShouldThrowException_WhenNotFound() {
        //
        when(repository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        // Act & Assert

        assertThrows(SagaEntityNotFoundException.class,
                () -> sagaProductCreation.createdInventoryEntry(PRODUCT_ID)
        );

        verify(repository, never()).save(any());
    }

    private void verifyStepUpdate(SagaStep expectedStep) {


        verify(repository, times(1)).save(
                argThat(
                        entity -> {

                            assertEquals(entity.getStep(), expectedStep);

                            assertEquals(PRODUCT_ID, entity.getId());

                            return entity.getId().equals(PRODUCT_ID) &&
                                    entity.getStep().equals(expectedStep);

                        }
                )
        );


    }

}
