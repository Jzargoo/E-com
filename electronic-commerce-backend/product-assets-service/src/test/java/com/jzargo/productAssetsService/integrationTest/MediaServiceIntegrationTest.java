package com.jzargo.productAssetsService.integrationTest;

import com.jzargo.productAssetsService.client.MediaServiceClientImpl;
import com.jzargo.productAssetsService.repository.MediaContentRepository;
import com.jzargo.productAssetsService.service.MediaServiceImpl;
import com.jzargo.protobuf.VersionedURI;
import org.junit.jupiter.api.Assertions;
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

    @Test
    public void test() {

        String uri = "key.png";

        String version = UUID.randomUUID().toString();

        Mockito.when(
                mediaServiceClient.sendFile(any(), any(), any())
        ).thenReturn(
                Mono.just(
                        VersionedURI.newBuilder()
                                .setUri(uri)
                                .setVersion(version)
                                .build()
                )
        );

        StepVerifier.create(
                        mediaService.addMediaContent(
                                Flux.empty(), 1L, 1, "image/png"
                        ).flatMap(
                                id -> mediaContentRepository.findById(id)
                        )
                )
                .assertNext(
                        mediaContent -> {
                            Assertions.assertNotNull(mediaContent);
                            Assertions.assertEquals(uri, mediaContent.getUri());
                            Assertions.assertEquals(version, mediaContent.getMediaVersion());
                            Assertions.assertEquals(1L, mediaContent.getProductAssetsId());

                            verify(
                                    mediaServiceClient,
                                    times(1)
                            ).sendFile(any(), any(), any());
                        }
                )
                .verifyComplete();
    }

}
