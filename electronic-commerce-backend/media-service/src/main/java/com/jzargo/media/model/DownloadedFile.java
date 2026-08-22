package com.jzargo.media.model;

import com.jzargo.protobuf.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DownloadedFile {
    InputStream content;

    Long contentLength;

    String fileUri;

    String versionId;

    ContentType contentType;
}
