package com.jzargo.media.unitTests;

import com.jzargo.media.config.ApplicationPropertyStorage;
import com.jzargo.media.config.balancing.MediaPersistentStorageBackendRegistry;
import com.jzargo.media.exceptions.FileAlreadyExistsException;
import com.jzargo.media.exceptions.CannotProcessException;
import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.model.DownloadedFile;
import com.jzargo.media.storages.persistent.MediaPersistentStorageBackendNative;
import com.jzargo.protobuf.ContentType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MediaPersistentStorageBackendNativeUnitTest {

    @InjectMocks
    public MediaPersistentStorageBackendNative mediaPersistentStorageBackendNative;

    @Mock
    public ApplicationPropertyStorage applicationPropertyStorage;

    @Mock
    public MediaPersistentStorageBackendRegistry mediaPersistentStorageBackendRegistry;

    @TempDir
    public Path tempPath;

    private byte[] content = new byte[] {0,1,2,3,4,5};

    private DownloadedFile file = DownloadedFile.builder()
            .fileUri("products/test1.png")
            .contentLength(
                    (long) content.length
            )
            .content(
                    new ByteArrayInputStream(content)
            )
            .contentType(ContentType.PNG)
            .versionId(
                    UUID.randomUUID().toString()
            )
            .build();


    @BeforeEach
    public void setup(){
        when(
                applicationPropertyStorage.getNativeStorageOptions()
        ).thenReturn(
                new ApplicationPropertyStorage.NativeStorageOptions(
                        tempPath.toString(), null, "versions", ".version", ".locks", ".lock"
                )
        );
    }

    @Test
    public void storeFile_success() throws WrongContentTypeException {


        try {

            String uri = mediaPersistentStorageBackendNative.storeFile(file);

            Assertions.assertEquals(
                    file.getFileUri(),
                    uri
            );

            Assertions.assertTrue(
                    mediaPersistentStorageBackendNative.existsByURI(uri)
            );

            Assertions.assertTrue(
                    mediaPersistentStorageBackendNative.existsByVersionedURI(
                            uri,
                            file.getVersionId()
                    )
            );

            DownloadedFile storedFile =
                    mediaPersistentStorageBackendNative.getFile(uri);

            Assertions.assertEquals(
                    file.getVersionId(),
                    storedFile.getVersionId()
            );

            Assertions.assertEquals(
                    file.getContentLength(),
                    storedFile.getContentLength()
            );

            Assertions.assertEquals(
                    file.getContentType(),
                    storedFile.getContentType()
            );

            byte[] actualContent;

            try (InputStream content = storedFile.getContent()) {
                actualContent = content.readAllBytes();
            }

            Assertions.assertArrayEquals(
                    content,
                    actualContent
            );



        } catch (CannotProcessException | IOException e) {
            Assertions.assertTrue(true, "store file threw exception, while it was expected that backend would save a file");
        }
    }

    @Test
    public void changeFile_whenFileContentWasChanged_successCase() throws CannotProcessException, IOException, FileAlreadyExistsException, WrongContentTypeException {
        String prevUri = mediaPersistentStorageBackendNative.storeFile(file);

        var content = new byte[]{1,2,3,4};

        DownloadedFile newFile = DownloadedFile.builder()
                .fileUri("products/test2.jpeg")
                .contentType(ContentType.JPEG)
                .versionId(
                        UUID.randomUUID().toString()
                )
                .content(
                        new ByteArrayInputStream(content)
                )
                .contentLength(
                        (long) content.length
                )
                .build();

        String uri =
                mediaPersistentStorageBackendNative.replaceFile(
                        newFile, file.getFileUri(), file.getVersionId()
                );

        Assertions.assertFalse(
                mediaPersistentStorageBackendNative.existsByURI(prevUri),
                "File exists while it was expected to be deleted");

        Assertions.assertEquals(newFile.getFileUri(), uri, "it should return file's uri");

        Assertions.assertTrue(
                mediaPersistentStorageBackendNative.existsByVersionedURI(
                        uri,
                        newFile.getVersionId()
                )
        );

        DownloadedFile storedFile =
                mediaPersistentStorageBackendNative.getFile(uri);

        Assertions.assertEquals(
                newFile.getVersionId(),
                storedFile.getVersionId()
        );

        Assertions.assertEquals(
                newFile.getContentLength(),
                storedFile.getContentLength()
        );

        Assertions.assertEquals(
                newFile.getContentType(),
                storedFile.getContentType()
        );

        byte[] actualContent;


        try (InputStream actContent = storedFile.getContent())  {

            actualContent = actContent.readAllBytes();
        }

        Assertions.assertArrayEquals(content, actualContent);

    }

}