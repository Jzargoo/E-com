package com.jzargo.media.helper;

import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.model.DownloadedFile;
import com.jzargo.protobuf.ContentType;
import com.jzargo.protobuf.PlainFile;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.progress.ProgressListener;
import org.apache.tika.Tika;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MediaHelper {
    static final Tika tika = new Tika();

    private MediaHelper() {}

    public static void checkContentType(PlainFile plainFile)
            throws WrongContentTypeException, IOException {

        String detect = tika.detect(plainFile.getContent().newInput());

        if (plainFile.getContentType() != parseContentType(detect)) {
            throw new WrongContentTypeException();
        }
    }

    private static ContentType parseContentType(String contentType) {
        return switch (contentType) {
            case "image/png" -> ContentType.PNG;
            case "image/jpeg" -> ContentType.JPEG;
            case "image/webp" -> ContentType.WEBP;
            case "video/webm" -> ContentType.WEBM;
            case "video/mp4" -> ContentType.MP4;
            default -> throw new WrongThreadException();
        };
    }

    private static boolean isVideo(ContentType contentType) {
        return contentType.equals(ContentType.MP4)  || contentType.equals(ContentType.WEBM);
    }

    private static byte[] getPosterFromVideo(byte[] bytes, String contentType) throws IOException {
        FFmpegBuilder builder = new FFmpegBuilder();

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        builder
                .setInput("pipe:0")
                .setFormat(contentType);

        builder
                .addOutput("pipe:1")
                .setFormat("image2")
                .setVideoCodec("mjpeg")
                .setFrames(1);

        FFmpegExecutor ffMpegExecutor = new FFmpegExecutor(
                new FFmpeg("ffmpeg")
        );

        ffMpegExecutor.createJob(builder);
    }

    public static DownloadedFile createFileRepresentation(
            ResponseInputStream<GetObjectResponse> stream,
            String contentType,
            String fileUri ) throws IOException {

        ContentType parsedContentType = parseContentType(contentType);
        if (isVideo(parsedContentType)) {
            return null;
        } else {
            return DownloadedFile.builder()
                    .fileUri(fileUri)
                    .contentType(parsedContentType)
                    .content(stream.readAllBytes())
                    .build();
        }
    }
}
