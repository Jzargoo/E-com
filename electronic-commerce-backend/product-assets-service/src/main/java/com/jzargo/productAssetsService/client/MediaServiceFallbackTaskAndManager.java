package com.jzargo.productAssetsService.client;

import com.jzargo.productAssetsService.driver.FallbackMediaDriver;
import com.jzargo.productAssetsService.entity.FallbackMediaContent;
import com.jzargo.productAssetsService.exception.CannotAddMediaFileException;
import com.jzargo.productAssetsService.exception.TaskCompletedException;
import com.jzargo.productAssetsService.mapper.MediaContentCreateMapper;
import com.jzargo.productAssetsService.model.PlainFile;
import com.jzargo.productAssetsService.repository.FallbackMediaContentRepository;
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
    private final MediaContentCreateMapper mediaContentCreateMapper;

    public MediaServiceFallbackTaskAndManager(
            FallbackMediaContentRepository fallbackMediaContentRepository,
            MediaServiceClient mediaServiceClient,
            FallbackMediaDriver fallbackMediaDriver,
            ProductRepository productRepository,
            MediaContentCreateMapper mediaContentCreateMapper) {

        this.fallbackMediaContentRepository = fallbackMediaContentRepository;
        this.mediaServiceClient = mediaServiceClient;
        this.fallbackMediaDriver = fallbackMediaDriver;
        this.productRepository = productRepository;
        this.mediaContentCreateMapper = mediaContentCreateMapper;
    }

    @Transactional
    public void task() throws TaskCompletedException, CannotAddMediaFileException {
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
                    fallbackMediaContent.getMediaUri()
            );

            if (fallbackMediaContent.getIsAvatar()) {

                uri = mediaServiceClient.sendFile(plainFile);

            } else {

                uri = mediaServiceClient.changeFile(
                        plainFile,
                        fallbackMediaContent.getMediaVersion(),
                        fallbackMediaContent.getProduct().getAvatar().getUri()
                );

            }

            fallbackMediaDriver.deleteFile(
                    fallbackMediaContent.getMediaUri()
            );

            fallbackMediaContent.getProduct().addMediaContent(
                    mediaContentCreateMapper.map(uri)
            );

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
