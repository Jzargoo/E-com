package com.jzargo.media.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@EnableAsync
@Configuration
@RequiredArgsConstructor
public class AsyncConfig {

    private final ApplicationPropertyStorage applicationPropertyStorage;

    @Bean("poster-executor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        var poster = applicationPropertyStorage.getAsync().getExecutor();

        executor.setCorePoolSize(poster.getCorePoolSize());
        executor.setMaxPoolSize(poster.getMaxPoolSize());
        executor.setQueueCapacity(poster.getQueueCapacity());
        executor.setKeepAliveSeconds(poster.getKeepAliveTimeInSeconds());
        executor.setThreadNamePrefix("poster-");

        executor.initialize();

        return executor;
    }
}
