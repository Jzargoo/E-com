package com.jzargo.media;

import com.jzargo.media.service.MediaStorageService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@EmbeddedKafka
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest({
                "spring.cloud.aws.parameterstore.enabled=true",
                "kafka.enabled=true"
})
class MediaServiceApplicationTests {

    @Container
    private static final MinIOContainer container = new MinIOContainer(
            DockerImageName.parse("pgsty/silo")
                    .asCompatibleSubstituteFor("minio/minio")
    );

    @BeforeAll
    public static void setup(){
        container
                .start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.s3.endpoint",  container::getS3URL);
        registry.add("spring.cloud.aws.credentials.access-key", container::getUserName);
        registry.add("spring.cloud.aws.credentials.secret-key", container::getPassword);
    }


    @AfterAll
    public static void teardown(){
        container.stop();
    }

    @Test
    void contextLoads() {}

}
