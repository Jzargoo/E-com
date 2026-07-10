package com.jzargo.media;

import com.google.protobuf.ByteString;
import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.helper.MediaHelper;
import com.jzargo.protobuf.ContentType;
import com.jzargo.protobuf.MediaFile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MediaHelperUnitTest {

    @TempDir
    public Path tempDir;

    @Test
    public void checkContentType_png_success_test() throws IOException {
        checkContentType_image_helper(ContentType.PNG, ContentType.PNG ,true, false);
    }

    @Test
    public void checkContentType_jpeg_success_test() throws IOException {
        checkContentType_image_helper(ContentType.JPEG, ContentType.JPEG, true, false);
    }

    @Test
    public void checkContentType_jpeg_fail_test() throws IOException {
        checkContentType_image_helper(ContentType.JPEG, ContentType.PNG, true, true);
    }


    // WEBP and video are not supported in java natively;
    // therefore, there is not a way to check for videos or webp images
    public void checkContentType_image_helper(ContentType contentType, ContentType requestType, boolean canCreate, boolean shouldThrows) throws IOException {

        Path test = Files.createTempFile(
                tempDir,
                "test",
                ".%s"
                        .formatted(contentType.toString().toLowerCase())
        );

        boolean success = transformFileToImage(
                test,
                contentType.toString(),
                new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
        );

        Assertions.assertEquals(canCreate, success, "PNG was not transformed!");

        try (var is = Files.newInputStream(test)) {

            MediaFile build = MediaFile.newBuilder()
                    .setContentType(requestType)
                    .setContentChunk(
                            ByteString.copyFrom(
                                    is.readNBytes(100)
                            )
                    )
                    .build();

            if(shouldThrows) {
                Assertions.assertThrows(WrongContentTypeException.class, () -> MediaHelper.checkContentType(build));
            } else {
                Assertions.assertDoesNotThrow(() -> MediaHelper.checkContentType(build));
            }

        }

    }

    private boolean transformFileToImage(Path path, String format, BufferedImage bi ) {

        File file = path.toFile();

        try {

            return ImageIO.write(bi, format, file);

        } catch (IOException e) {
            Assertions.fail(e.getMessage());
        }
        return false;
    }
}