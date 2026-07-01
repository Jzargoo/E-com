package com.jzargo.media.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "application")
public class ApplicationPropertyStorage {
    private NativeStorageOptions nativeStorageOptions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NativeStorageOptions {
        private String savingPath;
    }
}
