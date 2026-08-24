package com.jzargo.productAssetsService.config.scheduling;


import reactor.core.publisher.Mono;

@FunctionalInterface
public interface ReactiveScheduledTask {
    Mono<Void> execute();
}
