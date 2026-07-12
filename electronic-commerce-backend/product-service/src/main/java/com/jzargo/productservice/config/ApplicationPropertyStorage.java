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

    private Security security;



    @Data
    public static class Media{
        @NotNull
        private String path;
        @NotNull
        private String defaultAvatarUri;
    }


    @Data
    public static class Caching{
        @NotNull
        private CacheProperties categoryCacheProperties;

        @NotNull
        private CacheProperties productCacheProperties;

        public List<CacheProperties> getCaches() {
            return List.of(categoryCacheProperties, productCacheProperties);
        }

        @Data
        public static class CacheProperties {
            private String name;
            private Long expireAfterAccessInSeconds;
            private Long expireAfterWriteInSeconds;
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
}
