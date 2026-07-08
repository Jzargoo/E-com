package com.jzargo.media.config.balancing;

import com.jzargo.media.storages.persistent.MediaPersistentStorageBackend;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Configuration
public class MediaStorageConfiguration {

    @Bean
    @Primary
    public MediaPersistentStorageBackend mediaStorageService(
            List<MediaPersistentStorageBackend> services,
            MediaPersistentStorageBackendRegistry registry) {

        RoundRobinTargetSource roundRobinTargetSource =
                new RoundRobinTargetSource(services, MediaPersistentStorageBackend.class, registry);

        ProxyFactory proxyFactory = new ProxyFactory();

        proxyFactory.setTargetSource(roundRobinTargetSource);

        proxyFactory.addInterface(MediaPersistentStorageBackend.class);

        return (MediaPersistentStorageBackend) proxyFactory.getProxy();
    }
}
