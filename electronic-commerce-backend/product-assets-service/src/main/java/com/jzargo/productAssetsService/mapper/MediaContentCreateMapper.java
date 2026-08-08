package com.jzargo.productAssetsService.mapper;

import com.jzargo.core.mapper.Mapper;
import com.jzargo.productAssetsService.entity.MediaContent;
import org.springframework.stereotype.Component;

@Component
public class MediaContentCreateMapper implements Mapper<String , MediaContent> {
    @Override
    public MediaContent map(String from) {
        return MediaContent.builder()
                .uri(from)
                .build();
    }
}
