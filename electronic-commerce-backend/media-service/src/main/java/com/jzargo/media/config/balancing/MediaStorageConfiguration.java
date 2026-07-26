package com.jzargo.media.config.balancing;

import com.jzargo.media.storages.persistent.MediaPersistentStorageBackend;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


@Configuration
public class MediaStorageConfiguration {

    @Bean
    @Primary
    public MediaPersistentStorageBackend persistentMediaStorageService(
            MediaPersistentStorageBackendRegistry registry) {

        return new BalancingMediaPersistentStorageBackend(registry);

    }

}
