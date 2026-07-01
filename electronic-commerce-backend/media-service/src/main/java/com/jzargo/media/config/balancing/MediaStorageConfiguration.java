package com.jzargo.media.config.balancing;

import com.jzargo.media.service.MediaStorageService;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Configuration
public class MediaStorageConfiguration {

    @Bean
    @Primary
    public MediaStorageService mediaStorageService(List<MediaStorageService> services) {
        RoundRobinTargetSource roundRobinTargetSource = new RoundRobinTargetSource(services, MediaStorageService.class);

        ProxyFactory proxyFactory = new ProxyFactory();

        proxyFactory.setTargetSource(roundRobinTargetSource);

        proxyFactory.addInterface(MediaStorageService.class);

        return (MediaStorageService) proxyFactory.getProxy();
    }
}
