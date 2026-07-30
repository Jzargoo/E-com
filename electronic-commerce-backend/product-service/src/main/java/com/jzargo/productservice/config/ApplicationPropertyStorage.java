package com.jzargo.productservice.config;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Component
@Data
@RefreshScope
@Validated
@ConfigurationProperties(prefix = "application")
public class ApplicationPropertyStorage {

    @NotNull
    private Media media;

    @NotNull
    private Caching caching;

    private FallbackMedia fallbackMedia;

    private Security security;

    private Grpc grpc;

    @Data
    public static class Media{
        @NotNull
        private String path;
        @NotNull
        private String defaultAvatarUri;
    }


    @Data
    public static class Caching{
        @NotNull(message = "properties for category cache was missing")
        private CacheProperties categoryCacheProperties;

        @NotNull(message = "properties for product cache was missing")
        private CacheProperties productCacheProperties;

        public List<CacheProperties> getCaches() {
            return List.of(categoryCacheProperties, productCacheProperties);
        }

        @Data
        public static class CacheProperties {
            @NotNull(message = "name in cache properties was missing in the context")
            private String name;
            @NotNull(message = "expire after access properties was missing in the context")
            private Long expireAfterAccessInSeconds;
            @NotNull(message = "expire after write properties was missing in the context")
            private Long expireAfterWriteInSeconds;
            @NotNull(message = "max size for cache properties was missing in the context")
            private Integer maxSize;
        }
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Security {
        private String jwksUri;
        private String clientId;
        private String issuer;
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
