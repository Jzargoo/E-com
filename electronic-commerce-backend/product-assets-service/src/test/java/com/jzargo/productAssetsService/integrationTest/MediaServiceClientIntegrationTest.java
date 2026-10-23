package com.jzargo.productAssetsService.integrationTest;

import com.jzargo.productAssetsService.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.r2dbc.autoconfigure.DataR2dbcRepositoriesAutoConfiguration;
import org.springframework.boot.grpc.test.autoconfigure.LocalGrpcServerPort;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.metrics.KafkaMetricsAutoConfiguration;
import org.springframework.boot.r2dbc.autoconfigure.*;
import org.springframework.boot.r2dbc.autoconfigure.observation.R2dbcObservationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest({
        "kafka.enabled=false",
        "security.enabled=false",
        "kafka.topology.enabled=false",
        "database.enabled=false"
})
@ImportAutoConfiguration({
        KafkaAutoConfiguration.class,
        KafkaMetricsAutoConfiguration.class,
        R2dbcAutoConfiguration.class,
        R2dbcInitializationAutoConfiguration.class,
        R2dbcProxyAutoConfiguration.class,
        R2dbcTransactionManagerAutoConfiguration.class,
        R2dbcObservationAutoConfiguration.class,
        DataR2dbcRepositoriesAutoConfiguration.class
})
public class MediaServiceClientIntegrationTest {

    @LocalGrpcServerPort
    private int port;

    @MockitoBean AvatarRepository avatarRepository;
    @MockitoBean FallbackMediaContentRepository fallbackMediaContentRepository;
    @MockitoBean MediaContentRepository mediaContentRepository;
    @MockitoBean ProductAssetsRepository productAssetsRepository;
    @MockitoBean SagaProductAssetsRepository sagaProductAssetsRepository;

    @Test
    public void test() {

    }

}
