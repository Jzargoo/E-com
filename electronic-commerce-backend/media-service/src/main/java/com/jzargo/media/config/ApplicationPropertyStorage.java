package com.jzargo.media.config;

import com.jzargo.media.storages.persistent.StorageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@Component
@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "application")
public class ApplicationPropertyStorage {

    public static String STORAGES_PROPERTIES = "application.second-storages";

    private NativeStorageOptions nativeStorageOptions;
    private Aws aws;
    private Set<SecondStorage> storages;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NativeStorageOptions {
        private String savingPath;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Aws{
        private String bucketName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecondStorage {

        private StorageType storageType;
        private String consumerGroup;

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            SecondStorage that = (SecondStorage) o;
            return storageType == that.storageType;
        }

        @Override
        public int hashCode() {
            return Objects.hash(storageType);
        }
    }


}
