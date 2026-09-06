package com.jzargo.productAssetsService.unit;

import com.jzargo.productAssetsService.client.MediaServiceClient;
import com.jzargo.productAssetsService.client.MediaServiceFallbackTaskAndManager;
import com.jzargo.productAssetsService.driver.FallbackMediaDriver;
import com.jzargo.productAssetsService.entity.Avatar;
import com.jzargo.productAssetsService.entity.FallbackMediaContent;
import com.jzargo.productAssetsService.entity.MediaContent;
import com.jzargo.productAssetsService.exception.TaskCompletedException;
import com.jzargo.productAssetsService.repository.AvatarRepository;
import com.jzargo.productAssetsService.repository.FallbackMediaContentRepository;
import com.jzargo.productAssetsService.repository.MediaContentRepository;
import com.jzargo.protobuf.ContentType;
import com.jzargo.protobuf.VersionedURI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MediaServiceFallbackUnitTest {

    private final FallbackMediaContent build = FallbackMediaContent.builder()
            .queueId(1L)
            .mediaUri("")
            .contentType(ContentType.MP4)
            .build();


    @InjectMocks
    public MediaServiceFallbackTaskAndManager mediaServiceFallbackTaskAndManager;

    @Mock
    public FallbackMediaContentRepository fallbackMediaContentRepository;
    @Mock
    public MediaServiceClient mediaServiceClient;
    @Mock
    public FallbackMediaDriver fallbackMediaDriver;
    @Mock
    public MediaContentRepository mediaContentRepository;
    @Mock
    public AvatarRepository avatarRepository;

    @Test
    public void task_whenNoContent_throws(){

        Mockito.when(
                fallbackMediaContentRepository.findFirstByIsFreeIsTrue()
        ).thenReturn(Mono.empty());

        Mono<Void> task = mediaServiceFallbackTaskAndManager.task();

        StepVerifier
                .create(task)
                .expectError(TaskCompletedException.class)
                .verify();
    }

    @Test
    public void task_whenContent_locked_throws() {

        // ARRANGE \\

        Mockito.when(
                fallbackMediaContentRepository.findFirstByIsFreeIsTrue()
        ).thenReturn(Mono.just(build));

        Mockito.when(
                fallbackMediaContentRepository.lockProcessing(build.getQueueId())
        ).thenReturn(Mono.just(0));

        // ACT
        var returned = mediaServiceFallbackTaskAndManager.task();

        // VERIFY

        StepVerifier
                .create(returned)
                .expectError(IllegalStateException.class)
                .verify();

        verifyNoInteractions(
                mediaServiceClient,
                mediaContentRepository,
                avatarRepository
        );
    }

    @Test
    public void task_whenContent_newFile_fullPipeline_success(){

        // ARRANGE

        this.mock_takingContent_success();
        this.mock_Uploading_success();
        this.mock_creatingMediaContent_success();
        this.mock_finalizingContent_success();

        // ACT

        Mono<Void> task = mediaServiceFallbackTaskAndManager.task();

        // VERIFY

        StepVerifier
                .create(task)
                .verifyComplete();

        verify(fallbackMediaContentRepository)
                .findFirstByIsFreeIsTrue();

        verify(fallbackMediaContentRepository)
                .lockProcessing(build.getQueueId());

        verify(fallbackMediaDriver)
                .getFile(build.getMediaUri());

        verify(mediaServiceClient)
                .sendFile(
                        eq(build.getMediaUri()),
                        any(),
                        eq(build.getContentType())
                );

        verify(mediaContentRepository)
                .save(any(MediaContent.class));

        verify(fallbackMediaDriver)
                .deleteFile(anyString());

        verify(fallbackMediaContentRepository)
                .deleteById(build.getQueueId());
    }


    private void mock_takingContent_success(){

        Mockito.when(
                fallbackMediaContentRepository.findFirstByIsFreeIsTrue()
        ).thenReturn(Mono.just(build));

        Mockito.when(
                fallbackMediaContentRepository.lockProcessing(build.getQueueId())
        ).thenReturn(Mono.just(1));

    }

    private void mock_Uploading_success(){

        Mockito.when(
                fallbackMediaDriver.getFile(build.getMediaUri())
        ).thenReturn(
                Flux.just(
                        DefaultDataBufferFactory.sharedInstance
                                .wrap(new byte[1024])
                )
        );

        if (build.getPreviousMediaVersion() == null){

            Mockito.when(
                    mediaServiceClient.sendFile(any(), any(), any())
            ).thenReturn(
                    Mono.just(
                            VersionedURI.newBuilder()
                                    .setUri(
                                            build.getMediaUri()
                                    )
                                    .setVersion(
                                            UUID.randomUUID().toString()
                                    )
                                    .build()
                    )
            );

        } else {

            Mockito.when(
                    mediaServiceClient.changeFile(any(), any(), any(),any())
            ).thenReturn(
                    Mono.just(
                            VersionedURI.newBuilder()
                                    .setUri(
                                            build.getMediaUri()
                                    )
                                    .setVersion(
                                            UUID.randomUUID().toString()
                                    )
                                    .build()
                    )
            );

        }

    }

    private void mock_creatingMediaContent_success() {
        Mockito.when(
                mediaContentRepository.save(any(MediaContent.class))
                ).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    }

    private void mock_findingPreviousMediaContent_success() {

        Mockito.when(
                mediaContentRepository.findByUri(anyString())
        ).thenReturn(Mono.just(MediaContent.builder().build()));

    }

    private void mock_savingAvatar_success(){
        Mockito.when(
                avatarRepository.findById(anyLong())
        ).thenReturn(
                Mono.just(new Avatar())
        );

        Mockito.when(
                avatarRepository.save(any(Avatar.class))
        ).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    }

    private void mock_finalizingContent_success(){

        doNothing().when(
                fallbackMediaDriver
        ).deleteFile(anyString());

        Mockito.when(
                fallbackMediaContentRepository.deleteById(anyLong())
        ).thenReturn(
                Mono.fromRunnable(() -> {})
        );
    }

}