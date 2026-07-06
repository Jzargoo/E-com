package com.jzargo.media.model;

import com.jzargo.protobuf.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DownloadedFile {
    byte[] content;

    String fileUri;

    ContentType contentType;
}
