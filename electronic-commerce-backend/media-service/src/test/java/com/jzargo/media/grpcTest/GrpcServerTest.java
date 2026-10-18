package com.jzargo.media.grpcTest;

import com.google.protobuf.ByteString;
import com.jzargo.grpc.metadata.MetadataConstantKeys;
import com.jzargo.media.event.KafkaEventPublisher;
import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.storages.primary.MediaPrimaryStorageService;
import com.jzargo.protobuf.*;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.LocalGrpcServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static com.jzargo.protobuf.ContentType.JPEG;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@Slf4j
@ActiveProfiles("test")
@SpringBootTest(properties = {"kafka.enabled=false", "spring.cloud.aws.parameterstore.enabled=false"})
public class GrpcServerTest {
    private static final String KEY = "bucket/media/content.jpg";

    private final ConcurrentLinkedQueue<VersionedURI> queue = new ConcurrentLinkedQueue<>();

    @LocalGrpcServerPort
    private int port;

    @MockitoBean
    KafkaEventPublisher eventPublisher;
    @MockitoBean
    MediaPrimaryStorageService primaryStorageService;

    @Autowired
    @SuppressWarnings("unused")
    private MediaPrimaryStorageService mediaPrimaryStorageService;

    @Test
    @DisplayName("adding a file")
    public void test_sendingContent() throws WrongContentTypeException {

        String address =  String.format("localhost:%d", port);

        ManagedChannel channel = NettyChannelBuilder.forTarget(address).usePlaintext().build();

        try {

            Metadata metadata = new Metadata();

            metadata.put(
                    MetadataConstantKeys.fileMetadataKey,

                    MediaFileMetadata.newBuilder()
                            .setFileUri(KEY)
                            .setVersion(UUID.randomUUID().toString())
                            .setContentType(JPEG)
                            .build().toByteArray()
            );


            MediaServiceGrpc.MediaServiceStub stub = MediaServiceGrpc.newStub(channel);

            StreamObserver<VersionedURI> streamObserver = new StreamObserver<>() {

                @Override
                public void onNext(VersionedURI value) {
                    queue.add(value);
                }

                @Override
                public void onError(Throwable t) {
                }

                @Override
                public void onCompleted() {

                }

            };

            StreamObserver<MediaFile> requestObserver = stub.withInterceptors(
                    MetadataUtils.newAttachHeadersInterceptor(metadata)
            ).addMediaFile(streamObserver);

            sendContent(requestObserver);

            Awaitility.await()
                    .atMost(10, TimeUnit.SECONDS)
                    .until(() -> !queue.isEmpty());

            VersionedURI poll = queue.poll();

            assertNotNull(poll);

            Assertions.assertEquals(
                    KEY,
                    poll.getUri()
            );

            verify(
                    primaryStorageService,
                    times(1)
            ).uploadFullFile(any(), any());

        } finally {
            channel.shutdownNow();
        }
    }

    private void sendContent(StreamObserver<MediaFile> requestObserver) {

        BufferedImage bufferedImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try {
            ImageIO.write(bufferedImage, JPEG.toString(), byteArrayOutputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] byteArray = byteArrayOutputStream.toByteArray();


        requestObserver.onNext(
                MediaFile.newBuilder()
                        .setContentChunk(ByteString.copyFrom(byteArray))
                        .build()
        );

        requestObserver.onCompleted();
    }
}