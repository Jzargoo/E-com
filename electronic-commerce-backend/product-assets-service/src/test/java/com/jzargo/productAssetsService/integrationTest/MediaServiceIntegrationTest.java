package com.jzargo.productAssetsService.integrationTest;

import com.jzargo.productAssetsService.client.MediaServiceClientImpl;
import com.jzargo.productAssetsService.entity.MediaContent;
import com.jzargo.productAssetsService.entity.ProductAssets;
import com.jzargo.productAssetsService.repository.MediaContentRepository;
import com.jzargo.productAssetsService.repository.ProductAssetsRepository;
import com.jzargo.productAssetsService.service.MediaServiceImpl;
import com.jzargo.protobuf.VersionedURI;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest({
        "kafka.topology.enabled=false",
        "database.enabled=true"
})
@EmbeddedKafka(
        partitions = 2,
        topics = {"productdbserver1.public.saga_product_entities"}
)
@ActiveProfiles("test")
public class MediaServiceIntegrationTest{

    @Autowired
    MediaServiceImpl mediaService;

    @MockitoBean
    ReactiveJwtDecoder reactivejwtDecoder;
    @MockitoBean
    JwtDecoder jwtDecoder;
    @MockitoBean
    MediaServiceClientImpl mediaServiceClient;
    @Autowired
    private MediaContentRepository mediaContentRepository;
    @Autowired
    private ProductAssetsRepository productAssetsRepository;

    @Test
    @DisplayName("testing media content success")
    public void test_addMediaContent_success() {

        String version = UUID.randomUUID().toString();

        Mockito.when(
                mediaServiceClient.sendFile(any(), any(), any())
        ).thenAnswer(
                invocationOnMock -> Mono.just(
                        VersionedURI.newBuilder()
                                .setUri(invocationOnMock.getArgument(0, String.class))
                                .setVersion(version)
                                .build()
                )
        );

        Mono<ProductAssets> save = productAssetsRepository.save(
                ProductAssets.builder()
                        .productId(1L)
                        .shopId(1)
                        .build()
        );

        StepVerifier.create(
                save.then(
                        mediaService.addMediaContent(
                                Flux.empty(), 1L, 1, "image/png"
                        ).flatMap(
                                id -> mediaContentRepository.findById(id)
                        )
                )
            ).assertNext(
                        mediaContent -> {
                            Assertions.assertNotNull(mediaContent);
                            Assertions.assertNotNull(mediaContent.getUri());
                            Assertions.assertEquals(1L, mediaContent.getProductAssetsId());
                        }
            ).verifyComplete();

        verify(
                mediaServiceClient,
                times(1)
        ).sendFile(any(), any(), any());

        MediaContent result = mediaContentRepository
                .findByProductAssetsId(1L)
                .block();

        System.out.println(result);
    }

}
