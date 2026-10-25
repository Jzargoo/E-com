package com.jzargo.productAssetsService.integrationTest;

import com.jzargo.productAssetsService.entity.ProductAssets;
import com.jzargo.productAssetsService.exception.AssetsAlreadyExistsException;
import com.jzargo.productAssetsService.repository.ProductAssetsRepository;
import com.jzargo.productAssetsService.service.AssetsServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest({
        "kafka.topology.enabled=false",
        "database.enabled=true"
})
@EmbeddedKafka(
        partitions = 2,
        topics = {"productdbserver1.public.saga_product_entities"}
)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@EnableConfigurationProperties(LiquibaseProperties.class)
public class AssetsServiceIntegrationTest {

    @Autowired
    AssetsServiceImpl assetsServiceImpl;

    @MockitoBean
    ReactiveJwtDecoder reactivejwtDecoder;
    @MockitoBean
    JwtDecoder jwtDecoder;
    @Autowired
    private ProductAssetsRepository productAssetsRepository;

    @Test
    @DisplayName("testing successful initializing a product")
    public void test_initProductAssets_success() {
        StepVerifier
                .create(
                        assetsServiceImpl.initAssetsProduct(1L, 1)
                )
                .assertNext(
                        asset ->{

                            Assertions.assertNotNull(asset);

                            Assertions.assertEquals(1L, asset.getProductId());

                            Assertions.assertEquals(1, asset.getShopId());

                            Assertions.assertNotNull(asset.getId());
                        }
                ).verifyComplete();
    }

    @Test
    @DisplayName("testing failDuplicate in init product assets")
    public void test_initProductAssets_failDuplicate() {

        Mono<ProductAssets> then = productAssetsRepository.save(
                ProductAssets.builder()
                        .productId(1L)
                        .shopId(1)
                        .build()
        ).then(
                assetsServiceImpl.initAssetsProduct(1L, 1)
        );

        StepVerifier.create(then)
                .expectError(AssetsAlreadyExistsException.class)
                .verify();
    }

    @Test
    @DisplayName("testing failDuplicate in init product assets")
    public void test_compensateProductAssets_success() {
        Mono<Boolean> then = productAssetsRepository.save(
                ProductAssets.builder()
                        .productId(1L)
                        .shopId(1)
                        .build()
        ).then(
                assetsServiceImpl.initAssetsCompensation(1L)
        ).then(
                productAssetsRepository.existsByProductId(1L)
        );


        StepVerifier.create(then)
                .expectNext(false)
                .expectComplete()
                .verify();
    }
}