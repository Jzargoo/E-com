package com.jzargo.productservice.client;

import com.jzargo.productservice.driver.FallbackMediaDriver;
import com.jzargo.productservice.exception.TaskCompletedException;
import com.jzargo.productservice.model.PlainFile;
import com.jzargo.productservice.repository.FallbackMediaContentRepository;
import com.jzargo.productservice.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
public class MediaServiceFallbackTaskAndManager {

    private final FallbackMediaContentRepository fallbackMediaContentRepository;
    private final MediaServiceClient mediaServiceClient;
    private final FallbackMediaDriver fallbackMediaDriver;
    private final ProductRepository productRepository;

    public MediaServiceFallbackTaskAndManager(
            FallbackMediaContentRepository fallbackMediaContentRepository,
            MediaServiceClient mediaServiceClient,
            FallbackMediaDriver fallbackMediaDriver,
            ProductRepository productRepository
    ) {

        this.fallbackMediaContentRepository = fallbackMediaContentRepository;
        this.mediaServiceClient = mediaServiceClient;
        this.fallbackMediaDriver = fallbackMediaDriver;
        this.productRepository = productRepository;

    }

    @Transactional
    public void task() throws TaskCompletedException {
        FallbackMediaContent fallbackMediaContent =
                fallbackMediaContentRepository
                        .findFirstByMediaUriIsNotNull()
                        .orElseThrow(TaskCompletedException::new);

        // TODO: implement a gRPC existsByUri and implement this branch

        try (
                InputStream file = fallbackMediaDriver.getFile(fallbackMediaContent.getMediaUri())
        ) {

            String uri;

            PlainFile plainFile = new PlainFile(
                    file,
                    fallbackMediaContent.getContentType(),
                    fallbackMediaContent.getLength(),
                    fallbackMediaContent.getMediaUri()
            );

            if (fallbackMediaContent.getIsAvatar()) {

                uri = mediaServiceClient.sendFile(plainFile);

            } else {

                uri = mediaServiceClient.changeFile(plainFile);

            }

            fallbackMediaDriver.deleteFile(
                    fallbackMediaContent.getMediaUri()
            );

            fallbackMediaContent.getProduct().addMedia(uri);

            fallbackMediaContent.getProduct().removeFallback(
                    fallbackMediaContent
            );

            productRepository.save( fallbackMediaContent.getProduct() );

            fallbackMediaContentRepository.delete(fallbackMediaContent);

            log.info("Media Uploaded Successfully");

        } catch (IOException e) {
            log.error("Occurred an exception during scheduling. Cannot open input stream on file with uri {}",
                    fallbackMediaContent.getMediaUri(),
                    e
            );
        }
    }

}
