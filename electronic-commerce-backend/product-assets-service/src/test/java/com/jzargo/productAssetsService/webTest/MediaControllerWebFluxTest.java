package com.jzargo.productAssetsService.webTest;


import com.jzargo.productAssetsService.api.MediaController;
import com.jzargo.productAssetsService.config.ApplicationPropertyStorage;
import com.jzargo.productAssetsService.exception.AssetNotFoundException;
import com.jzargo.productAssetsService.exception.CreatedInFallbackException;
import com.jzargo.productAssetsService.helper.ContentTypeParser;
import com.jzargo.productAssetsService.model.PlainFile;
import com.jzargo.productAssetsService.service.MediaServiceImpl;
import com.jzargo.protobuf.ContentType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@WebFluxTest(controllers = MediaController.class)
@ExtendWith(MockitoExtension.class)
public class MediaControllerWebFluxTest {
    @MockitoBean
    public MediaServiceImpl mediaService;

    @MockitoBean
    public ApplicationPropertyStorage applicationPropertyStorage;

    @Autowired
    WebTestClient webTestClient;

    @Test
    public void test_getIdsByProductId_success(){

        when(
                mediaService.findIdsByProductId(1L)
        ).thenReturn(
                Flux.just(
                        1L, 2L, 3L
                )
        );

        webTestClient.get()
                .uri(URI.create("/api/media/1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$[0]").isEqualTo(1L)
                .jsonPath("$[1]").isEqualTo(2L)
                .jsonPath("$[2]").isEqualTo(3L);

    }

    @Test
    public void test_getAssetsByAssetId_success(){

        byte[] content = new byte[]{12};

        Arrays.fill(content, (byte) 12);

        when(
                mediaService.getMediaContent(anyLong())
        ).thenReturn(
                PlainFile.builder()
                        .contentType(
                                Mono.just(ContentType.PNG)
                        )
                        .upload(
                                Flux.just(
                                        DefaultDataBufferFactory.sharedInstance.wrap(content)
                                )
                        )
                        .build()
        );

        byte[] responseBody = webTestClient.get()
                .uri(URI.create("/api/media/assets/2"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(ContentTypeParser.parseIntoMime(ContentType.PNG))
                .expectBody()
                .returnResult()
                .getResponseBody();

        Assertions.assertArrayEquals(content, responseBody, "Array's content did not match");
    }

    @Test
    public void test_globalHandler_for_getIdsByProductId_noContent(){

        when(
                mediaService.findIdsByProductId(anyLong())
        ).thenReturn(
                Flux.error(
                        new AssetNotFoundException("[TEST] Asset not found as expected!")
                )
        );

        webTestClient.get()
                .uri(URI.create("/api/media/1"))
                .exchange()
                .expectStatus().isEqualTo(
                        HttpStatus.NOT_FOUND.value()
                );

    }

    @Test
    public void test_globalHandler_for_createdInFallback(){

        when(
                mediaService.addMediaContent(any(), any(), any(), any())
        ).thenReturn(
                Mono.error(
                        new CreatedInFallbackException()
                )
        );

        webTestClient
                .mutateWith(
                        mockJwt()
                                .jwt(
                                        jwt -> jwt
                                                .claim("shop_id", 1)
                                                .build()
                                )
                )
                .put()
                .uri(URI.create("/api/media/1"))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(
                        BodyInserters.fromDataBuffers(
                                Flux.just(
                                        DefaultDataBufferFactory.sharedInstance.wrap(
                                                "hello".getBytes(StandardCharsets.UTF_8)
                                        )
                                )
                        )
                )
                .exchange()
                .expectStatus().isCreated();

    }
}
