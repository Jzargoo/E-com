package com.jzargo.productservice.service;

import com.jzargo.productservice.client.MediaServiceFallbackTaskAndManager;
import com.jzargo.productservice.config.scheduling.DynamicTaskExecutionRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SchedulingServiceImpl implements SchedulingService {


    private final MediaServiceFallbackTaskAndManager mediaServiceFallbackTaskAndManager;
    private final DynamicTaskExecutionRegistry dynamicTaskExecutionRegistry;

    public SchedulingServiceImpl(MediaServiceFallbackTaskAndManager mediaServiceFallbackTaskAndManager, DynamicTaskExecutionRegistry dynamicTaskExecutionRegistry) {
        this.mediaServiceFallbackTaskAndManager = mediaServiceFallbackTaskAndManager;
        this.dynamicTaskExecutionRegistry = dynamicTaskExecutionRegistry;
    }

    @Override
    public void turnOnFallbackMediaScheduling() {
        dynamicTaskExecutionRegistry.addTask(
                MediaServiceFallbackTaskAndManager.class.getSimpleName(),
                mediaServiceFallbackTaskAndManager::task,
                5000L
        );
    }

    @Override
    public void turnOffFallbackMediaScheduling() {
        dynamicTaskExecutionRegistry.removeTask(
                MediaServiceFallbackTaskAndManager.class.getSimpleName()
        );

    }

    @PostConstruct
    public void init() {
        log.debug("Turning on fallback media scheduling tasks...");
        turnOnFallbackMediaScheduling();
    }
}
