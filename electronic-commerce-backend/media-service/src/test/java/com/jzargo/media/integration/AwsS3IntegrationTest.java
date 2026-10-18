package com.jzargo.media.integration;

import com.jzargo.media.config.ApplicationPropertyStorage;
import com.jzargo.media.event.KafkaDlqEventPublisher;
import com.jzargo.media.event.KafkaEventPublisher;
import com.jzargo.media.exceptions.CannotDownloadFileException;
import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.model.DownloadedFile;
import com.jzargo.media.service.MediaStorageService;
import com.jzargo.media.storages.primary.MediaPrimaryStorageService;
import com.jzargo.protobuf.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest({
        "kafka.enabled=false",
        "spring.cloud.aws.parameterstore.enabled=true"
})
@Testcontainers
@ActiveProfiles("test")
public class AwsS3IntegrationTest {

    @Container
    private static final MinIOContainer container = new MinIOContainer(
            DockerImageName.parse("pgsty/silo")
                    .asCompatibleSubstituteFor("minio/minio")
    );

    @MockitoBean
    private KafkaEventPublisher kafkaEventPublisher;

    @MockitoBean
    private KafkaDlqEventPublisher kafkaDlqEventPublisher;

    @Autowired
    private MediaPrimaryStorageService mediaPrimaryStorageService;
    @MockitoSpyBean
    private S3Client s3Client;
    @Autowired
    private ApplicationPropertyStorage applicationPropertyStorage;

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
    @DisplayName("test putting full file")
    public void test_putFullFile_success() throws WrongContentTypeException {

        // ARRANGE

        ByteArrayInputStream content = new ByteArrayInputStream(
                new byte [1024 * 1024]
        );

        DownloadedFile build = DownloadedFile.builder()
                .fileUri("media.png")
                .contentType(ContentType.PNG)
                .contentLength(1024 * 1024L)
                .content(content)
                .versionId(UUID.randomUUID().toString())
                .build();

        // ACT
        String version = mediaPrimaryStorageService.uploadFullFile(
                build, Optional.empty()
        );

        // VERIFY
        verify(
                s3Client,
                times(1)
        ).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        assertEquals(build.getVersionId(), version);

        HeadObjectResponse headObjectResponse = s3Client.headObject(
                HeadObjectRequest.builder()
                        .bucket(
                                applicationPropertyStorage.getAws().getBucket()
                        )
                        .key(build.getFileUri())
                        .build()
        );

        assertNotNull(headObjectResponse);
    }

    @Test
    @DisplayName("test downloading full file")
    public void test_downloadFile_success() throws CannotDownloadFileException, IOException, WrongContentTypeException {

        ByteArrayInputStream content = new ByteArrayInputStream(
                new byte [1024 * 1024]
        );

        DownloadedFile build = DownloadedFile.builder()
                .fileUri("media.png")
                .contentType(ContentType.PNG)
                .contentLength(1024 * 1024L)
                .content(content)
                .versionId(UUID.randomUUID().toString())
                .build();

        String version = mediaPrimaryStorageService.uploadFullFile(
                build, Optional.empty()
        );

        // ACT

        DownloadedFile downloadedFile = mediaPrimaryStorageService.downloadFile(build.getFileUri());

        // ASSERT
        assertEquals(build.getVersionId(), version);
        assertEquals(build.getFileUri(), downloadedFile.getFileUri());
        assertEquals(build.getVersionId(), downloadedFile.getVersionId());
        assertEquals(build.getContentType(), downloadedFile.getContentType());
        assertEquals(build.getContentLength(), downloadedFile.getContentLength());
    }

    @Test
    @DisplayName("test abort multipart success")
    public void test_startMultipart_success() throws WrongContentTypeException {

        String version = UUID.randomUUID().toString();
        String key = "media.mp4";

        String uploadId = mediaPrimaryStorageService.startUploadingFile(
                ContentType.MP4,
                key, version
        );

        assertNotNull(uploadId);

        verify(
                s3Client, times(1)
        ).createMultipartUpload( any(CreateMultipartUploadRequest.class) );

        ListMultipartUploadsResponse listMultipartUploadsResponse = s3Client.listMultipartUploads(
                ListMultipartUploadsRequest.builder()
                        .bucket(
                                applicationPropertyStorage.getAws().getBucket()
                        )
                        .prefix(key)
                        .build()
        );

        boolean b = listMultipartUploadsResponse.uploads().stream()
                .anyMatch((multipartUpload) -> multipartUpload.uploadId().equals(uploadId));

        assertTrue(b, "upload is missing in the list of uploads!");
    }

    @Test
    @DisplayName("test abort multipart success")
    public void test_abortMultipart_success() throws WrongContentTypeException {

        // ARRANGE
        String version = UUID.randomUUID().toString();
        String key = "media.mp4";

        String uploadId = mediaPrimaryStorageService.startUploadingFile(
                ContentType.MP4,
                key, version
        );

        // ACT
        mediaPrimaryStorageService.abortMultipartFile(key,  uploadId);

        //ASSERT

        verify(
                s3Client, times(1)
        ).abortMultipartUpload( any(AbortMultipartUploadRequest.class) );

        ListMultipartUploadsResponse listMultipartUploadsResponse = s3Client.listMultipartUploads(
                ListMultipartUploadsRequest.builder()
                        .bucket(
                                applicationPropertyStorage.getAws().getBucket()
                        )
                        .prefix(key)
                        .build()
        );

        boolean b = listMultipartUploadsResponse.uploads().stream()
                .anyMatch((multipartUpload) -> multipartUpload.uploadId().equals(uploadId));

        assertFalse(b, "upload presents in the list even after abort!");
    }
}