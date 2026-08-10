package com.jzargo.productAssetsService.config;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;


@Component
@Data
@RefreshScope
@Validated
@ConfigurationProperties(prefix = "application")
public class ApplicationPropertyStorage {

    private Media media;

    private FallbackMedia fallbackMedia;

    private Grpc grpc;

    @Data
    public static class Media{
        private String path;

        private String defaultAvatarUri;
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Grpc {
        private String channelMediaName;
        private Integer portionSize;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FallbackMedia {
        private Integer portionSize;
    }

}
