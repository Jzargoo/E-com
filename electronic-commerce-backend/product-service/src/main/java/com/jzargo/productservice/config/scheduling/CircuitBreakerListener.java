package com.jzargo.productservice.config.scheduling;

import com.jzargo.productservice.service.SchedulingService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Slf4j
@Configuration
public class CircuitBreakerListener {
    @Bean
    public RegistryEventConsumer<CircuitBreaker> taskConsumer(SchedulingService service) {
        return new  RegistryEventConsumer<>() {

            @Override
            public void onEntryAddedEvent(@NonNull EntryAddedEvent<CircuitBreaker> entryAddedEvent) {

                CircuitBreaker addedEntry = entryAddedEvent.getAddedEntry();


                addedEntry.getEventPublisher()
                        .onStateTransition(
                                event -> {
                                    log.trace("CircuitBreaker added transition event: {}", event);


                                    if (
                                            event.getStateTransition().getFromState() == CircuitBreaker.State.CLOSED &&
                                                    event.getStateTransition().getToState() == CircuitBreaker.State.OPEN

                                    ) {
                                        log.warn("Circuit breaker had been opened. Turning off media scheduling");
                                        service.turnOffFallbackMediaScheduling();

                                    } else if (

                                            event.getStateTransition().getFromState() == CircuitBreaker.State.HALF_OPEN &&
                                                    event.getStateTransition().getToState() == CircuitBreaker.State.CLOSED

                                    ){

                                        log.info("Circuit breaker had been closed. Turning on media scheduling");
                                        service.turnOnFallbackMediaScheduling();

                                    }
                                }
                        );

            }

            @Override
            public void onEntryRemovedEvent(@NonNull EntryRemovedEvent<CircuitBreaker> entryRemoveEvent) {
                log.trace("CircuitBreakerListener::onEntryRemovedEvent");
            }

            @Override
            public void onEntryReplacedEvent(@NonNull EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
                log.trace("CircuitBreakerListener::onEntryReplacedEvent");
            }

        };

    }

}
