package com.jzargo.productservice.saga;

import com.jzargo.productservice.repository.SagaProductCreationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class SagaExpirationScheduler {
    private final SagaProductCreationRepository sagaProductCreationRepository;

    public SagaExpirationScheduler(SagaProductCreationRepository sagaProductCreationRepository) {
        this.sagaProductCreationRepository = sagaProductCreationRepository;
    }

    @Transactional
    @Scheduled(fixedDelayString = "#{applicationPropertyStorage.saga.schedulerDelay}")
    public void sagaExpiration() {
        Integer columns = sagaProductCreationRepository.checkAndSetExpiration(
                "Saga was expired by timeout"
        );

        log.debug("scheduler checked whether saga entity is expired and found {} expired entities", columns);
    }
}
