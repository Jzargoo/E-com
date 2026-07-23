package com.jzargo.media.helper;

import com.jzargo.media.exceptions.CannotProcessException;
import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.model.DownloadedFile;
import com.jzargo.protobuf.ContentType;
import com.jzargo.protobuf.MediaFile;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

@Slf4j
public class MediaHelper {
    static final Tika tika = new Tika();

    private MediaHelper() {}

    public static void checkContentType(MediaFile mediaFile)
            throws WrongContentTypeException, IOException {

        String detect = tika.detect(mediaFile.getContentChunk().newInput());

        if (mediaFile.getContentType() != parseContentType(detect)) {
            throw new WrongContentTypeException();
        }
    }

    private static ContentType parseContentType(String contentType) throws WrongContentTypeException {
        return switch (contentType) {
            case "image/png" -> ContentType.PNG;
            case "image/jpeg" -> ContentType.JPEG;
            case "image/webp" -> ContentType.WEBP;
            case "video/webm" -> ContentType.WEBM;
            case "video/mp4" -> ContentType.MP4;
            default -> throw new WrongContentTypeException();
        };
    }

    public static boolean isVideo(ContentType contentType) {
        return contentType.equals(ContentType.MP4)  || contentType.equals(ContentType.WEBM);
    }

    public static DownloadedFile getPosterFromVideo(InputStream bytes, ContentType contentType) throws IOException, CannotProcessException, WrongContentTypeException {

        if (
                isVideo(contentType)
        ) {
            throw new WrongContentTypeException();
        }

        FFmpegFrameGrabber fFmpegFrameGrabber = new FFmpegFrameGrabber(bytes);

        fFmpegFrameGrabber.start();

        Frame frame;

        while ((frame = fFmpegFrameGrabber.grabImage()) != null) {
            if (frame.image != null) {
                break;
            }
        }

        if (frame == null) {
            fFmpegFrameGrabber.stop();
            throw new CannotProcessException();
        }


        try(Java2DFrameConverter converter = new Java2DFrameConverter()) {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            BufferedImage image = converter.convert(frame);

            ImageIO.write(image, "jpeg", baos);


            return DownloadedFile.builder()
                    .contentType(ContentType.JPEG)
                    .contentLength((long) baos.size())
                    .content(
                            new ByteArrayInputStream(
                                    baos.toByteArray()
                            )
                    )
                    .build();

        }

    }

    public static DownloadedFile createFileRepresentation(
            InputStream stream,
            Long contentLength,
            String contentType,
            String fileUri ) throws IOException, CannotProcessException, WrongContentTypeException {

        ContentType parsedContentType = parseContentType(contentType);

        return DownloadedFile.builder()
                .content(stream)
                .fileUri(fileUri)
                .contentLength(contentLength)
                .contentType(parsedContentType)
                .build();
    }

    public static String getMediaPostfix(ContentType contentType) throws WrongContentTypeException {
        return switch (contentType){
            case PNG -> "png";
            case JPEG -> "jpeg";
            case WEBP -> "webp";
            case WEBM -> "webm";
            case MP4 -> "mp4";
            default -> throw new WrongThreadException();
        };
    }

    public static ContentType getTypeByPostfix(String postfix) {
        return switch (postfix){
            case "png" -> ContentType.PNG;
            case "jpeg" ->  ContentType.JPEG;
            case  "webp" -> ContentType.WEBP;
            case  "webm" -> ContentType.WEBM;
            case "mp4" ->  ContentType.MP4;
            default -> throw new WrongThreadException();
        };

    }

    public static String parseToMime(ContentType contentType) throws WrongContentTypeException {
        return switch (contentType){
            case PNG -> "image/png";
            case JPEG -> "image/jpeg";
            case WEBP -> "image/webp";
            case WEBM -> "video/webm";
            case MP4 -> "video/mp4";
            default -> throw new WrongThreadException();
        };
    }

}
