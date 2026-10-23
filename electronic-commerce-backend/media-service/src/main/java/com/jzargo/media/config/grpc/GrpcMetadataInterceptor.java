package com.jzargo.media.config.grpc;

import com.google.protobuf.InvalidProtocolBufferException;
import com.jzargo.grpc.metadata.MetadataConstantKeys;
import com.jzargo.media.helper.MediaHelper;
import com.jzargo.protobuf.ChangeMediaFileMetadata;
import com.jzargo.protobuf.MediaFileMetadata;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@GlobalServerInterceptor
public class GrpcMetadataInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {

        Context context = Context.current();

        log.info("Intercepting request");

        if (
                metadata.containsKey(
                        MetadataConstantKeys.fileMetadataKey
                )
        ) {
            log.info("Received request for file metadata");

            try {

                context = putFileMetadataToContext(metadata, context);

                log.info("Putting file metadata to context");

            } catch (InvalidProtocolBufferException e) {

                serverCall.close(
                        Status.ABORTED.augmentDescription("Error while parsing file metadata."),
                        metadata
                );

                log.error("Closing stream");

                return new ServerCall.Listener<>() {};

            }

        } else {
            context = context.withValue(
                    MetadataConstantKeys.fileContextKey,
                    new AtomicReference<>()
            );
        }

        if (
                metadata.containsKey(
                        MetadataConstantKeys.fileVersionMetadataKey
                )
        ) {
            try {

                context = putVersionMetadataToContext(metadata, context);

                log.info("Putting version metadata to context");

            } catch (InvalidProtocolBufferException e) {

                serverCall.close(
                        Status.ABORTED.augmentDescription("Error while parsing file metadata."),
                        metadata
                );

                log.error("Closing a stream");

                return new ServerCall.Listener<>() {};

            }
        }

        ServerCall<ReqT, RespT> wrappedCall = getReqTRespTServerCall(serverCall, context);

        return Contexts.interceptCall(
                context, wrappedCall, metadata, serverCallHandler
        );

    }

    private static <ReqT, RespT> @NonNull ServerCall<ReqT, RespT> getReqTRespTServerCall(ServerCall<ReqT, RespT> serverCall, Context finalContext) {

        return new ForwardingServerCall.SimpleForwardingServerCall<>(serverCall) {

            @Override
            public void sendHeaders(Metadata responseHeaders) {

                log.info("Sending headers");

                MediaFileMetadata fileMeta = MetadataConstantKeys.fileContextKey.get(finalContext).get();

                if (fileMeta != null) {

                    responseHeaders.put(

                            MetadataConstantKeys.fileMetadataKey,

                            MediaFileMetadata.newBuilder()
                                    .setFileUri(fileMeta.getFileUri())
                                    .setContentType(
                                            fileMeta.getContentType()
                                    )
                                    .build().toByteArray()
                    );

                    log.info("putted headers");
                }

                super.sendHeaders(responseHeaders);

            }

        };

    }

    private Context putVersionMetadataToContext(Metadata metadata, Context context) throws InvalidProtocolBufferException {

        if (
                metadata.containsKey(
                        MetadataConstantKeys.fileVersionMetadataKey
                )
        ) {
            byte[] bytes = metadata.get(
                    MetadataConstantKeys.fileVersionMetadataKey
            );


            ChangeMediaFileMetadata changeMediaFileMetadata = ChangeMediaFileMetadata.parseFrom(bytes);

            return context.withValue(
                    MetadataConstantKeys.versionContextKey,
                    changeMediaFileMetadata
            );

        }

        return context;
    }

    private Context putFileMetadataToContext(Metadata metadata, Context context) throws  InvalidProtocolBufferException {
        byte[] bytes = metadata.get(
                MetadataConstantKeys.fileMetadataKey
        );


        MediaFileMetadata mediaFileMetadata = MediaFileMetadata.parseFrom(bytes);


        return context.withValue(
                MetadataConstantKeys.fileContextKey,
                new AtomicReference<>(mediaFileMetadata)
        );
    }

}