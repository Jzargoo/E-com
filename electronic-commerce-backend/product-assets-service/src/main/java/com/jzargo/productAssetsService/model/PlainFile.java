package com.jzargo.productAssetsService.model;

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
public class PlainFile {
    private InputStream is;
    private ContentType contentType;
    private String uri;
}
