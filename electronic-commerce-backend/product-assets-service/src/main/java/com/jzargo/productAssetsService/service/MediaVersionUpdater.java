package com.jzargo.productAssetsService.service;

import com.jzargo.productAssetsService.repository.MediaContentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
public class MediaVersionUpdater {

    private final MediaContentRepository mediaContentRepository;

    public MediaVersionUpdater(MediaContentRepository mediaContentRepository) {
        this.mediaContentRepository = mediaContentRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Mono<Integer> updateVersionByUri(String uri, String version) {
        return mediaContentRepository.updateVersion(uri, version);
    }

}
