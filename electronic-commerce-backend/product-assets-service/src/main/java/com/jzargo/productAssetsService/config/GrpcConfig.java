package com.jzargo.productAssetsService.config;

import com.jzargo.protobuf.MediaServiceGrpc;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
@ConditionalOnBooleanProperty("grpc.enabled")
public class GrpcConfig {

    @Bean
    MediaServiceGrpc.MediaServiceStub  mediaServiceStub(
            GrpcChannelFactory grpcChannelFactory,
            ApplicationPropertyStorage applicationPropertyStorage) {

        return MediaServiceGrpc.newStub(
                grpcChannelFactory.createChannel(
                        applicationPropertyStorage.getGrpc().getChannelMediaName()
                )
        );
    }
}
