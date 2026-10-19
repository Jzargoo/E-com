package com.jzargo.productAssetsService.config.grpc;

import com.jzargo.grpc.metadata.MetadataConstantKeys;
import com.jzargo.protobuf.MediaFileMetadata;
import io.grpc.*;
import lombok.SneakyThrows;
import org.springframework.grpc.client.GlobalClientInterceptor;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
@GlobalClientInterceptor
public class GrpcClientMetadataInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {

            final AtomicReference<MediaFileMetadata> holder = callOptions.getOption(MetadataConstantKeys.mediaFileOptions);

            @Override
            public void start(Listener<RespT> responseListener, Metadata headersClient) {

                super.start(
                        new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {

                            @SneakyThrows
                            @Override
                            public void onHeaders(Metadata headersServer) {

                                byte[] byteArray = headersServer.get(MetadataConstantKeys.fileMetadataKey);

                                if (holder != null && byteArray != null) {

                                    holder.set(
                                            MediaFileMetadata.parseFrom(byteArray)
                                    );

                                }

                                super.onHeaders(headersServer);
                            }

                        },
                        headersClient
                );

            }

        };
    }

}
