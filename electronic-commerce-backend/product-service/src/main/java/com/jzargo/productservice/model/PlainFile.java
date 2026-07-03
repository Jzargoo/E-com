package com.jzargo.productservice.model;

import com.jzargo.protobuf.ContentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlainFile {
    private byte[] content;
    private ContentType contentType;

}
