package com.jzargo.grpc.metadata;

import com.jzargo.protobuf.ChangeMediaFileMetadata;
import com.jzargo.protobuf.MediaFileMetadata;
import io.grpc.Context;
import io.grpc.Metadata;

public class  MetadataConstantKeys {
    public static final Metadata.Key<byte[]> fileMetadataKey =
            Metadata.Key.of("file-metadata" + Metadata.BINARY_HEADER_SUFFIX, Metadata.BINARY_BYTE_MARSHALLER);

    public static final Metadata.Key<byte[]> fileVersionMetadataKey =
            Metadata.Key.of("version-metadata" + Metadata.BINARY_HEADER_SUFFIX, Metadata.BINARY_BYTE_MARSHALLER);

    public static final Context.Key<MediaFileMetadata> fileContextKey =
            Context.key("file-metadata-key");

    public static final Context.Key<ChangeMediaFileMetadata> versionContextKey =
            Context.key("version-metadata-key");

}