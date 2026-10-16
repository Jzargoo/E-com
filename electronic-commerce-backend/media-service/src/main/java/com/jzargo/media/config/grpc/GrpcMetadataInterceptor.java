package com.jzargo.media.config.grpc;

import com.google.protobuf.InvalidProtocolBufferException;
import com.jzargo.grpc.metadata.MetadataConstantKeys;
import com.jzargo.media.helper.MediaHelper;
import com.jzargo.protobuf.ChangeMediaFileMetadata;
import com.jzargo.protobuf.MediaFileMetadata;
import io.grpc.*;
import org.jspecify.annotations.NonNull;
import org.springframework.grpc.server.GlobalServerInterceptor;

import java.util.concurrent.atomic.AtomicReference;

@GlobalServerInterceptor
public class GrpcMetadataInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {

        Context finalContext = Context.current();

        if (
                metadata.containsKey(
                        MetadataConstantKeys.fileMetadataKey
                ) ||
                        metadata.containsKey(
                                MetadataConstantKeys.fileVersionMetadataKey
                        )
        ) {

            Context context = Context.current();

            try {

                context = putFileMetadataToContext(metadata, context);

                context = putVersionMetadataToContext(metadata, context);


            } catch (InvalidProtocolBufferException e) {

                serverCall.close(
                        Status.ABORTED.augmentDescription("Error while parsing file metadata."),
                        metadata
                );

                return new ServerCall.Listener<>() {};

            }

            finalContext = context;
        }

        ServerCall<ReqT, RespT> wrappedCall = getReqTRespTServerCall(serverCall, finalContext);

        return Contexts.interceptCall(
                finalContext, wrappedCall, metadata, serverCallHandler
        );

    }

    private static <ReqT, RespT> @NonNull ServerCall<ReqT, RespT> getReqTRespTServerCall(ServerCall<ReqT, RespT> serverCall, Context finalContext) {

        return new ForwardingServerCall.SimpleForwardingServerCall<>(serverCall) {

            @Override
            public void sendHeaders(Metadata responseHeaders) {

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

        if (
                metadata.containsKey(
                        MetadataConstantKeys.fileMetadataKey
                )
        ) {
            byte[] bytes = metadata.get(
                    MetadataConstantKeys.fileMetadataKey
            );


            MediaFileMetadata mediaFileMetadata = MediaFileMetadata.parseFrom(bytes);


            return context.withValue(
                    MetadataConstantKeys.fileContextKey,
                    new AtomicReference<>(mediaFileMetadata)
            );

        }

        return context;
    }

}