package com.jzargo.productAssetsService.config.scheduling;

import com.jzargo.productservice.exception.TaskCompletedException;

@FunctionalInterface
public interface ScheduledTask {
    void execute() throws TaskCompletedException;
}
