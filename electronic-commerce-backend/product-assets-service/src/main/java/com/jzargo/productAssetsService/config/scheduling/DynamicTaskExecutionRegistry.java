package com.jzargo.productAssetsService.config.scheduling;

import com.jzargo.productAssetsService.exception.TaskCompletedException;
import com.jzargo.productAssetsService.helper.GlobalLogger;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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

    public void addTask(String taskId, ReactiveScheduledTask task, Long delay){

        log.debug("Adding task {} to the active tasks queue", taskId);

        ScheduledFuture<?> scheduledFuture =
                threadPoolTaskScheduler.scheduleWithFixedDelay(
                        () -> executeTask(task, taskId),
                        Duration.ofMillis(delay)
                );

        activeTasks.put(taskId, scheduledFuture);

        log.trace("Added task {} to the active tasks queue", taskId);

    }

    private void executeTask(ReactiveScheduledTask task, String taskId) {

        task.execute()
                .doOnError(

                throwable -> {

                    if (
                            throwable instanceof TaskCompletedException ||
                                    throwable instanceof StatusRuntimeException

                    ){

                        log.info("Task {} has been completed or client unavailable", taskId);

                        removeTask(taskId);

                    } else {
                        GlobalLogger.logException(throwable, throwable.getMessage());
                    }

                })

                .onErrorResume(TaskCompletedException.class, e -> Mono.empty())

                .onErrorResume(StatusRuntimeException.class,  e -> Mono.empty())

                .subscribe();

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
