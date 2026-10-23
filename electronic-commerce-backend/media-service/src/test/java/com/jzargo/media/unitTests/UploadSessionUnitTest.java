package com.jzargo.media.unitTests;

import com.google.protobuf.ByteString;
import com.jzargo.media.exceptions.CannotProcessException;
import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.helper.MediaHelper;
import com.jzargo.media.model.DownloadedFile;
import com.jzargo.media.service.MediaStorageService;
import com.jzargo.media.service.TempFileBufferFactory;
import com.jzargo.media.service.UploadSession;
import com.jzargo.protobuf.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.com.google.common.io.BaseEncoding;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UploadSessionUnitTest {

    @Mock
    private TempFileBufferFactory.TempFileBuffer buffer;

    @Mock
    private MediaStorageService mediaStorageService;

    @Test
    @DisplayName("upload session: process, one file")
    public void testUploadSession_process_inOneFile() throws IOException, CannotProcessException {

        String version = UUID.randomUUID().toString();
        ContentType contentType = ContentType.WEBM;
        String key = "bucket/hello/world.webm";

        UploadSession uploadSession = new UploadSession(
                version, buffer, contentType, mediaStorageService, MediaHelper.isVideo(contentType), key
        );

        when(
                buffer.addChunkRespectToThreshold(any())
        ).thenReturn(false);

        uploadSession.process(
                MediaFile.newBuilder()
                        .setContentChunk(ByteString.copyFrom(new byte[1024]))
                        .build()
        );

        verifyNoInteractions(mediaStorageService);
    }

    @Test
    @DisplayName("upload session: process, multipart")
    public void testUploadSession_process_multipart() throws IOException, CannotProcessException, WrongContentTypeException {

        String version = UUID.randomUUID().toString();
        ContentType contentType = ContentType.WEBM;
        String key = "bucket/hello/world.webm";

        UploadSession uploadSession = new UploadSession(
                version, buffer, contentType, mediaStorageService, MediaHelper.isVideo(contentType), key
        );

        when(
                buffer.addChunkRespectToThreshold(any())
        ).thenReturn(true);

        when(
                mediaStorageService.initiateFile(any(), any(), any())
        ).thenReturn(UUID.randomUUID().toString());

        String tag = UUID.randomUUID().toString();

        when(
                mediaStorageService.storeChunkFile(
                        any(), any(), any(),
                        any(), any()
                )
        ).thenReturn(tag);

        uploadSession.process(
                MediaFile.newBuilder()
                        .setContentChunk(ByteString.copyFrom(new byte[1024]))
                        .build()
        );

        verify(
                mediaStorageService, times(1)
        ).initiateFile(any(), any(), any());

        verify(
                mediaStorageService, times(1)
        ).storeChunkFile(
                any(), any(), any(),
                any(), any()
        );
    }

// ---------- complete ----------

    @Test
    @DisplayName("upload session: complete, multipart")
    public void testUploadSession_complete_multipart() throws IOException, CannotProcessException, WrongContentTypeException {

        String version = UUID.randomUUID().toString();
        ContentType contentType = ContentType.WEBM;
        String key = "bucket/hello/world.webm";
        String uploadId = UUID.randomUUID().toString();

        UploadSession uploadSession = new UploadSession(
                version, buffer, contentType, mediaStorageService, MediaHelper.isVideo(contentType), key
        );

        when(buffer.addChunkRespectToThreshold(any())).thenReturn(true);
        when(mediaStorageService.initiateFile(any(), any(), any())).thenReturn(uploadId);
        when(mediaStorageService.storeChunkFile(any(), any(), any(), any(), any()))
                .thenReturn(UUID.randomUUID().toString());

        uploadSession.process(
                MediaFile.newBuilder()
                        .setContentChunk(ByteString.copyFrom(new byte[1024]))
                        .build()
        );

        VersionedURI result = uploadSession.complete();

        verify(mediaStorageService, times(1))
                .finishFileUploading(eq(key), eq(uploadId), any(), eq(true));
        verify(mediaStorageService, never()).storeFullFile(any());

        assertEquals(key, result.getUri());
        assertEquals(version, result.getVersion());
    }

    @Test
    @DisplayName("upload session: complete, single file")
    public void testUploadSession_complete_single() throws IOException, CannotProcessException, WrongContentTypeException {

        String version = UUID.randomUUID().toString();
        ContentType contentType = ContentType.JPEG;
        String key = "bucket/hello/world.jpg";

        UploadSession uploadSession = new UploadSession(
                version, buffer, contentType, mediaStorageService, MediaHelper.isVideo(contentType), key
        );

        when(buffer.addChunkRespectToThreshold(any())).thenReturn(false);
        when(buffer.getSize()).thenReturn(1024L);

        uploadSession.process(
                MediaFile.newBuilder()
                        .setContentChunk(ByteString.copyFrom(new byte[1024]))
                        .build()
        );

        VersionedURI result = uploadSession.complete();

        verify(mediaStorageService, times(1)).storeFullFile(any(DownloadedFile.class));
        verify(mediaStorageService, never()).initiateFile(any(), any(), any());
        verify(mediaStorageService, never()).finishFileUploading(any(), any(), any(), anyBoolean());

        assertEquals(key, result.getUri());
        assertEquals(version, result.getVersion());
    }

    @Test
    @DisplayName("upload session: abort, multipart")
    public void test_abort_success() throws IOException, CannotProcessException, WrongContentTypeException {

        ContentType contentType = ContentType.WEBM;
        String key = "bucket/hello/world.webm";
        String uploadId = UUID.randomUUID().toString();

        UploadSession uploadSession = new UploadSession(
                UUID.randomUUID().toString(), buffer, contentType, mediaStorageService,
                MediaHelper.isVideo(contentType), key
        );

        when(buffer.addChunkRespectToThreshold(any())).thenReturn(true);
        when(mediaStorageService.initiateFile(any(), any(), any())).thenReturn(uploadId);
        when(mediaStorageService.storeChunkFile(any(), any(), any(), any(), any()))
                .thenReturn(UUID.randomUUID().toString());

        uploadSession.process(
                MediaFile.newBuilder()
                        .setContentChunk(ByteString.copyFrom(new byte[1024]))
                        .build()
        );

        uploadSession.abort();

        verify(buffer, times(1)).close();
        verify(mediaStorageService, times(1)).abortMultipartFile(key, uploadId);
    }

    @Test
    @DisplayName("upload session: abort, single file")
    public void testUploadSession_abort_single() throws IOException, CannotProcessException, WrongContentTypeException {

        ContentType contentType = ContentType.JPEG;

        UploadSession uploadSession = new UploadSession(
                UUID.randomUUID().toString(), buffer, contentType, mediaStorageService,
                MediaHelper.isVideo(contentType), "bucket/hello/world.jpg"
        );

        when(buffer.addChunkRespectToThreshold(any())).thenReturn(false);

        uploadSession.process(
                MediaFile.newBuilder()
                        .setContentChunk(ByteString.copyFrom(new byte[1024]))
                        .build()
        );

        uploadSession.abort();

        verify(buffer, times(1)).close();
        verify(mediaStorageService, never()).abortMultipartFile(any(), any());
    }

    @Test
    @DisplayName("upload session: abort, buffer close throws IOException")
    public void testUploadSession_abort_bufferCloseFails() throws IOException, WrongContentTypeException, CannotProcessException {

        ContentType contentType = ContentType.JPEG;

        UploadSession uploadSession = new UploadSession(
                UUID.randomUUID().toString(), buffer, contentType, mediaStorageService,
                MediaHelper.isVideo(contentType), "bucket/hello/world.jpg"
        );

        when(buffer.addChunkRespectToThreshold(any())).thenReturn(false);

        doThrow(new IOException("close failed")).when(buffer).close();

        uploadSession.process(
                MediaFile.newBuilder()
                        .setContentChunk(ByteString.copyFrom(new byte[1024]))
                        .build()
        );

        assertThrows(CannotProcessException.class, uploadSession::abort);
    }

    @Test
    public void test_base64() {
        MediaFileMetadata build = MediaFileMetadata.newBuilder()
                .setContentType(ContentType.PNG)
                .setVersion(
                        UUID.randomUUID().toString()
                )
                .setFileUri(
                        "ftoc.png"
                ).build();

        String encode = BaseEncoding.base64().encode(
                build.toByteArray()
        );

        ChangeMediaFileMetadata change = ChangeMediaFileMetadata.newBuilder()
                .setPreviousUri("ftoc.png")
                .setPreviousVersion("b978bbe9-f9a3-4c5c-b9cf-a9c8a6fb0c76")
                .build();

        String encodeChange = BaseEncoding.base64().encode(
                change.toByteArray()
        );

        System.out.println(encodeChange);
    }
}