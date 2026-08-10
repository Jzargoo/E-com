package com.jzargo.productAssetsService.config.scheduling;


import com.jzargo.productAssetsService.exception.CannotAddMediaFileException;
import com.jzargo.productAssetsService.exception.TaskCompletedException;

@FunctionalInterface
public interface ScheduledTask {
    void execute() throws TaskCompletedException, CannotAddMediaFileException;
}
