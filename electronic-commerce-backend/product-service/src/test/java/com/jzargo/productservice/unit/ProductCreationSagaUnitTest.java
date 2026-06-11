package com.jzargo.productservice.unit;

import com.jzargo.productservice.entity.SagaProductEntity;
import com.jzargo.productservice.entity.SagaStep;
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
                12.0,
                HashMap.newHashMap(1),
                "Good product",
                "Electronics"
        );


        product = SagaProductEntity.builder()
                .id(PRODUCT_ID)
                .build();
    }

    @Test
    public void  test_initiatedProductCreation_success(){
        // Arrange

        when(
                productService.createProduct(details)
        ).thenReturn(PRODUCT_ID);

        // Act
        sagaProductCreation.initiatedProductCreation(details);

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
        sagaProductCreation.initiatedInventoryEntry(PRODUCT_ID);

        // Assert
        verifyStepUpdate(SagaStep.PENDING_PRICE);
    }

    @Test
    @DisplayName("Should update step to FINISHED when media is initiated")
    void initiatedMediaEntry_Success() throws SagaEntityNotFoundException {
        when(repository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        sagaProductCreation.initiatedMediaEntry(PRODUCT_ID);

        verifyStepUpdate(SagaStep.FINISHED);
    }

    @Test
    @DisplayName("Should update step to FAILED when media is compensated")
    void compensatedMediaEntry_Success() throws SagaEntityNotFoundException {
        when(repository.findById(PRODUCT_ID)).thenReturn(Optional.ofNullable(product));

        sagaProductCreation.compensatedMediaEntry(PRODUCT_ID);

        verifyStepUpdate(SagaStep.FAILED);
    }

    @Test
    @DisplayName("Should throw SagaEntityNotFoundException when product is missing")
    void anyMethod_ShouldThrowException_WhenNotFound() {
        //
        when(repository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        // Act & Assert

        assertThrows(SagaEntityNotFoundException.class,
                () -> sagaProductCreation.initiatedInventoryEntry(PRODUCT_ID)
        );

        verify(repository, never()).save(any());
    }

    private void verifyStepUpdate(SagaStep expectedStep) {

        ArgumentCaptor<SagaProductEntity> captor = ArgumentCaptor.forClass(SagaProductEntity.class);

        verify(repository, times(1)).save(captor.capture());

        SagaProductEntity entity = captor.getValue();

        assertEquals(entity.getStep(), expectedStep);
        assertEquals(PRODUCT_ID, entity.getId());
    }

}
