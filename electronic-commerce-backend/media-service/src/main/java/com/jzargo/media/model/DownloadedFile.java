package com.jzargo.media.model;

import com.jzargo.protobuf.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DownloadedFile {
    InputStream content;

    Long contentLength;

    String fileUri;

    ContentType contentType;
}
