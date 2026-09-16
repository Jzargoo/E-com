package com.jzargo.media.config;

import com.google.protobuf.InvalidProtocolBufferException;
import com.jzargo.grpc.metadata.MetadataConstantKeys;
import com.jzargo.protobuf.ChangeMediaFileMetadata;
import com.jzargo.protobuf.MediaFileMetadata;
import io.grpc.*;
import org.springframework.grpc.server.GlobalServerInterceptor;

@GlobalServerInterceptor
public class GrpcMetadataInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {

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

                putFileMetadataToContext(metadata, context);

                putVersionMetadataToContext(metadata, context);


            } catch (InvalidProtocolBufferException e) {

                serverCall.close(
                        Status.ABORTED.augmentDescription("Error while parsing file metadata."),
                        metadata
                );
            }



            return Contexts.interceptCall(
                    context, serverCall, metadata, serverCallHandler
            );

        }

        return interceptCall(serverCall, metadata, serverCallHandler);

    }

    private void putVersionMetadataToContext(Metadata metadata, Context context) throws InvalidProtocolBufferException {

        if (
                metadata.containsKey(
                        MetadataConstantKeys.fileVersionMetadataKey
                )
        ) {
            byte[] bytes = metadata.get(
                    MetadataConstantKeys.fileVersionMetadataKey
            );


            ChangeMediaFileMetadata changeMediaFileMetadata = ChangeMediaFileMetadata.parseFrom(bytes);




            context.withValue(
                    MetadataConstantKeys.versionContextKey,
                    changeMediaFileMetadata
            );

        }

    }

    private void putFileMetadataToContext(Metadata metadata, Context context) throws  InvalidProtocolBufferException {

        if (
                metadata.containsKey(
                        MetadataConstantKeys.fileMetadataKey
                )
        ) {
            byte[] bytes = metadata.get(
                    MetadataConstantKeys.fileMetadataKey
            );


            MediaFileMetadata mediaFileMetadata = MediaFileMetadata.parseFrom(bytes);


            context.withValue(
                    MetadataConstantKeys.fileContextKey,
                    mediaFileMetadata
            );

        }

    }

}