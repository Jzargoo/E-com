package com.jzargo.productAssetsService.unit;

import com.jzargo.productAssetsService.client.MediaServiceClient;
import com.jzargo.productAssetsService.driver.FallbackMediaDriver;
import com.jzargo.productAssetsService.entity.MediaContent;
import com.jzargo.productAssetsService.entity.ProductAssets;
import com.jzargo.productAssetsService.exception.AssetNotFoundException;
import com.jzargo.productAssetsService.exception.CreatedInFallbackException;
import com.jzargo.productAssetsService.exception.ShopDoesNotOwnProductException;
import com.jzargo.productAssetsService.helper.ContentTypeParser;
import com.jzargo.productAssetsService.helper.PlainFileConverter;
import com.jzargo.productAssetsService.repository.AvatarRepository;
import com.jzargo.productAssetsService.repository.FallbackMediaContentRepository;
import com.jzargo.productAssetsService.repository.MediaContentRepository;
import com.jzargo.productAssetsService.repository.ProductAssetsRepository;
import com.jzargo.productAssetsService.service.MediaContentUploadService;
import com.jzargo.productAssetsService.service.MediaServiceImpl;
import com.jzargo.protobuf.ContentType;
import com.jzargo.protobuf.VersionedURI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MediaServiceUnitTest {

    private final static long PRODUCT_ID = 1L;
    private final static int SHOP_ID = 2;


    @InjectMocks
    public MediaServiceImpl mediaService;

    @Mock
    public MediaServiceClient mediaServiceClient;
    @Mock
    public MediaContentUploadService mediaContentUploadService;
    @Mock
    public FallbackMediaContentRepository fallbackMediaContentRepository;
    @Mock
    public FallbackMediaDriver fallbackMediaDriver;
    @Mock
    public ProductAssetsRepository productAssetsRepository;
    @Mock
    public MediaContentRepository mediaContentRepository;
    @Mock
    public PlainFileConverter plainFileConverter;
    @Mock
    public AvatarRepository avatarRepository;

    public Mono<ProductAssets> assets = Mono.just(
            ProductAssets.builder()
                    .id(1L)
                    .productId(PRODUCT_ID)
                    .shopId(SHOP_ID)
                    .build()
    );


    @Test
    public void findIdsByProductId_whenNoContent_throws(){

        when(
                mediaContentRepository.findAllByProductAssetsId(anyLong())
        ).thenReturn(
                Flux.empty()
        );

        when(
                productAssetsRepository.findByProductId(anyLong())
        ).thenReturn(assets);

        Flux<Long> idsByProductId = mediaService.findIdsByProductId(1L);

        StepVerifier
                .create(idsByProductId)
                .expectError(AssetNotFoundException.class)
                .verify();

    }

    @Test
    public void findIdsByProductId_whenContent_success(){

        when(
                productAssetsRepository.findByProductId(anyLong())
        ).thenReturn(assets);

        when(
                mediaContentRepository.findAllByProductAssetsId(anyLong())
        ).thenReturn(
                Flux.just(
                        MediaContent.builder()
                                .id(1L)
                                .build(),
                        MediaContent.builder()
                                .id(2L)
                                .build(),
                        MediaContent.builder()
                                .id(3L)
                                .build()
                        )
        );

        Flux<Long> idsByProductId = mediaService.findIdsByProductId(1L);

        StepVerifier
                .create(idsByProductId)
                .expectNextCount(3L)
                .expectComplete()
                .verify();
    }

    @Test
    public void addMediaContent_whenClientStable_success(){


        Flux<DataBuffer> content = Flux.just(
                DefaultDataBufferFactory.sharedInstance.wrap(new byte[1024])
        );

        when(
                productAssetsRepository.findByProductIdAndShopId(PRODUCT_ID, SHOP_ID)
        ).thenReturn(
                Mono.just(
                        ProductAssets.builder()
                                .id(1L)
                                .shopId(SHOP_ID)
                                .productId(PRODUCT_ID)
                                .build()
                )
        );

        when(
                mediaContentUploadService.saveAndUpload(any(), any(), any())
        ).thenReturn(
                Mono.just(
                        new MediaContentUploadService.MediaUploadResult(
                                1L, 1L, UUID.randomUUID().toString()
                        )
                )
        );

        when(
                mediaContentRepository.findByUri(anyString())
        ).thenReturn(
                Mono.just(
                        MediaContent.builder()
                                .id(1L)
                                .uri("1.png")
                                .build()
                )
        );

        when(
                mediaContentRepository.updateVersion(any(), any())
        ).thenReturn(Mono.just(1));

        Mono<Long> longMono = mediaService.addMediaContent(
                content,
                PRODUCT_ID,
                SHOP_ID,
                ContentTypeParser.parseIntoMime(ContentType.PNG)
        );

        StepVerifier
                .create(longMono)
                .expectNext(1L)
                .expectComplete()
                .verify();

        verify(
                mediaContentUploadService,
                times(1)
        ).saveAndUpload(any(), any(), any());

        verify(
                mediaContentRepository,
                times(1)
        ).updateVersion(any(), any());
    }

    @Test
    public void addMediaContent_whenClientUnstable_throws() throws IOException {

        Flux<DataBuffer> content = Flux.just(
                DefaultDataBufferFactory.sharedInstance.wrap(new byte[1024])
        );

        when(
                productAssetsRepository.findByProductIdAndShopId(PRODUCT_ID, SHOP_ID)
        ).thenReturn(
                Mono.just(
                        ProductAssets.builder()
                                .id(1L)
                                .shopId(SHOP_ID)
                                .productId(PRODUCT_ID)
                                .build()
                )
        );

        when(
                fallbackMediaDriver.saveFile(any(), any())
        ).thenReturn(
                Mono.just("")
        );

        when(
                fallbackMediaContentRepository.save(any())
        ).thenAnswer(
                invocation -> Mono.just(invocation.getArgument(0))
        );

        Mono<Long> longMono = mediaService.fallbackAddMediaContent(
                content,
                PRODUCT_ID,
                SHOP_ID,
                ContentTypeParser.parseIntoMime(ContentType.MP4)
        );

        StepVerifier
                .create(longMono)
                .expectError(CreatedInFallbackException.class)
                .verify();

    }

    @Test
    public void getMediaContent_whenValidationFails_throws(){

        Flux<DataBuffer> content = Flux.just(
                DefaultDataBufferFactory.sharedInstance.wrap(new byte[1024])
        );

        when(
                productAssetsRepository.findByProductIdAndShopId(PRODUCT_ID, SHOP_ID)
        ).thenReturn(
                Mono.just(
                        ProductAssets.builder()
                                .shopId(SHOP_ID + 10)
                                .productId(PRODUCT_ID)
                                .build()
                )
        );

        Mono<Long> longMono = mediaService.addMediaContent(
                content,
                PRODUCT_ID,
                SHOP_ID,
                ContentTypeParser.parseIntoMime(ContentType.PNG)
        );

        StepVerifier
                .create(longMono)
                .expectError(ShopDoesNotOwnProductException.class)
                .verify();
    }
}