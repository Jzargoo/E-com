package com.jzargo.productAssetsService.config.scheduling;

import com.jzargo.productAssetsService.exception.TaskCompletedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
public class DynamicTaskExecutionRegistry {
    private final ThreadPoolTaskScheduler threadPoolTaskScheduler;

    private final ConcurrentHashMap<String, ScheduledFuture<?>> activeTasks =
            new ConcurrentHashMap<>();

    public DynamicTaskExecutionRegistry(ThreadPoolTaskScheduler threadPoolTaskScheduler) {
        this.threadPoolTaskScheduler = threadPoolTaskScheduler;
    }

    public void addTask(String taskId, ScheduledTask task, Long delay){

        log.debug("Adding task {} to the active tasks queue", taskId);

        ScheduledFuture<?> scheduledFuture =
                threadPoolTaskScheduler.scheduleWithFixedDelay(
                        () -> executeTask(task, taskId),
                        Duration.ofMillis(delay)
                );

        activeTasks.put(taskId, scheduledFuture);

        log.trace("Added task {} to the active tasks queue", taskId);

    }

    private void executeTask(ScheduledTask task, String taskId) {

        try {

            task.execute();

        } catch (TaskCompletedException e) {

            log.info("A task was completed successfully! Removing from registry ...");

            removeTask(taskId);

        } catch (Exception e) {
            log.error("Error occurred while processing a task with id {}!", taskId, e);
        }

    }

    public void removeTask(String taskId) {

        log.info("Removing task with id {}", taskId);

        ScheduledFuture<?> scheduledFuture = activeTasks.remove(taskId);

        if (scheduledFuture != null) {

            scheduledFuture.cancel(false);

            log.trace("Removed task with id {}", taskId);

        }

    }
}
