package com.jzargo.productservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("media-service-executor")
    public Executor mediaServiceExecutor(ApplicationPropertyStorage applicationPropertyStorage) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        ApplicationPropertyStorage.Async.AsyncProperties mediaServiceExecutor =
                applicationPropertyStorage.getAsync().getMediaServiceExecutor();

        executor.setCorePoolSize(mediaServiceExecutor.getCorePoolSize());
        executor.setMaxPoolSize(mediaServiceExecutor.getMaxPoolSize());
        executor.setQueueCapacity(mediaServiceExecutor.getQueueCapacity());
        executor.setThreadNamePrefix("media-service-executor-");

        executor.initialize();

        return  executor;
    }
}
