package com.jzargo.productAssetsService.service;


import com.jzargo.productAssetsService.client.MediaServiceFallbackTaskAndManager;
import com.jzargo.productAssetsService.config.scheduling.DynamicTaskExecutionRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.debug("Turning on fallback media scheduling tasks...");
        turnOnFallbackMediaScheduling();
    }

}
