package com.jzargo.productservice.model;

import com.jzargo.protobuf.ContentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlainFile {
    private InputStream content;
    private ContentType contentType;
    private Long length;
}
