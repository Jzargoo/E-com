package com.jzargo.productAssetsService.unit;

import com.jzargo.productAssetsService.entity.MediaContent;
import com.jzargo.productAssetsService.entity.ProductAssets;
import com.jzargo.productAssetsService.exception.AssetNotFoundException;
import com.jzargo.productAssetsService.repository.MediaContentRepository;
import com.jzargo.productAssetsService.repository.ProductAssetsRepository;
import com.jzargo.productAssetsService.service.AssetsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.parameters.P;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductAssetsServiceUnitTest {

    private int SHOP_ID  = 1;
    private long PRODUCT_ID = 12L;
    private String ERROR_MESSAGE = "Test exception";

    @Mock
    public ProductAssetsRepository productAssetsRepository;
    @Mock
    public MediaContentRepository mediaContentRepository;

    @InjectMocks
    public AssetsServiceImpl assetsService;

    @Test
    public void initAssetsProduct_success() {

        ProductAssets build = ProductAssets.builder()
                .productId(PRODUCT_ID)
                .shopId(SHOP_ID)
                .build();


        when(
                productAssetsRepository.save(build)
        ).thenReturn(
                Mono.just(build)
        );


        Mono<ProductAssets> productAssetsMono = assetsService.initAssetsProduct(PRODUCT_ID, SHOP_ID);

        StepVerifier
                .create(productAssetsMono)
                .expectNext(build)
                .verifyComplete();
    }

    @Test
    public void initAssetsProduct_whenDidNotSave_fail() {

        when(
                productAssetsRepository.save(any())
        ).thenReturn(
                Mono.error(new DataIntegrityViolationException(ERROR_MESSAGE))
        );

        Mono<ProductAssets> productAssetsMono = assetsService.initAssetsProduct(PRODUCT_ID, SHOP_ID);

        StepVerifier
                .create(productAssetsMono)
                .expectError(DataIntegrityViolationException.class)
                .verify();


    }

    @Test
    public void initAssetsCompensation_success() {

        // ARRANGE

        ProductAssets input = new ProductAssets(PRODUCT_ID, SHOP_ID);

        when(
                productAssetsRepository.findById(anyLong())
        ).thenReturn(
                Mono.just(input)
        );

        when(
                mediaContentRepository.findAllByProductId(PRODUCT_ID)
        ).thenReturn(
                Flux.just(
                        MediaContent.builder().build(), MediaContent.builder().build()
                )
        );

        when(
                mediaContentRepository.delete(any())
        ).thenReturn(
                Mono.fromRunnable(() -> {})
        );

        when(
               productAssetsRepository.delete(any())
        ).thenReturn(
                Mono.fromRunnable(() -> {})
        );


        // ACT

        Mono<Void> voidMono = assetsService.initAssetsCompensation(PRODUCT_ID);


        // VERIFICATION

        StepVerifier
                .create(voidMono)
                .expectComplete()
                .verify();



        verify(productAssetsRepository).findById(anyLong());

        verify(mediaContentRepository).findAllByProductId(anyLong());

        verify(mediaContentRepository, times(2)).delete(any());

        verify(productAssetsRepository).delete(any());

    }

    @Test
    public void initCompensation_NotFound_throws() {
        when(
                productAssetsRepository.findById(anyLong())
        ).thenReturn(Mono.empty());

        Mono<Void> voidMono = assetsService.initAssetsCompensation(PRODUCT_ID);

        StepVerifier
                .create(voidMono)
                .expectError(AssetNotFoundException.class)
                .verify();
    }

}
