package com.jzargo.productAssetsService.model;

import com.jzargo.protobuf.ContentType;
import lombok.*;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlainFile {
    private Flux<DataBuffer> upload;
    private Mono<ContentType> contentType;
}
