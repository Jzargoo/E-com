package com.jzargo.media.grpcTest;

import com.jzargo.grpc.metadata.MetadataConstantKeys;
import com.jzargo.media.event.KafkaEventPublisher;
import com.jzargo.media.storages.primary.MediaPrimaryStorageService;
import com.jzargo.protobuf.ContentType;
import com.jzargo.protobuf.MediaFileMetadata;
import com.jzargo.protobuf.MediaServiceGrpc;
import com.jzargo.protobuf.VersionedURI;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.grpc.test.autoconfigure.LocalGrpcServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(properties = "kafka.enabled=false")
public class GrpcServerTest {

    @LocalGrpcServerPort
    private int port;

    @MockitoBean
    KafkaEventPublisher eventPublisher;
    @MockitoBean
    MediaPrimaryStorageService primaryStorageService;

    @Test
    public void test_sendingContent() {

        log.info("Starting test GrpcServer. port is {}", port);

        String address =  String.format("localhost:%d", port);

        ManagedChannel channel = NettyChannelBuilder.forTarget(address).usePlaintext().build();

        try{
            String key = "bucket/media/content.jpg";

            Metadata metadata = new Metadata();

            metadata.put(
                    MetadataConstantKeys.fileMetadataKey,

                    MediaFileMetadata.newBuilder()
                            .setFileUri(key)
                            .setContentType(ContentType.JPEG)
                            .build().toByteArray()
            );


            MediaServiceGrpc.MediaServiceStub stub = MediaServiceGrpc.newStub(channel);

            StreamObserver<VersionedURI> streamObserver = new StreamObserver<>() {

                @Override
                public void onNext(VersionedURI value) {

                }

                @Override
                public void onError(Throwable t) {
                }

                @Override
                public void onCompleted() {

                }

            };

            stub.withInterceptors(
                    MetadataUtils.newAttachHeadersInterceptor(metadata)
            ).addMediaFile()

        } finally {
            channel.shutdownNow();
        }
    }
}
