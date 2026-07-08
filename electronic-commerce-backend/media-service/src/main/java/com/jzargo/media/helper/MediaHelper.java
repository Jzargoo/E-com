package com.jzargo.media.helper;

import com.jzargo.media.exceptions.CannotProcessException;
import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.model.DownloadedFile;
import com.jzargo.protobuf.ContentType;
import com.jzargo.protobuf.MediaFile;
import org.apache.tika.Tika;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

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

    private static void getPosterFromVideo(InputStream bytes, OutputStream os, ContentType contentType) throws IOException, CannotProcessException, WrongContentTypeException {

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

        try(Java2DFrameConverter converter = new Java2DFrameConverter()){
            BufferedImage convert = converter.convert(frame);

            ImageIO.write(convert, "png", os);

        } finally {
            fFmpegFrameGrabber.stop();
        }
    }

    public static DownloadedFile createFileRepresentation(
            ResponseInputStream<GetObjectResponse> stream,
            String contentType,
            String fileUri ) throws IOException, CannotProcessException, WrongContentTypeException {

        ContentType parsedContentType = parseContentType(contentType);

        OutputStream outputStream = new ByteArrayOutputStream();

        if (isVideo(parsedContentType)) {

            getPosterFromVideo(stream, outputStream, parsedContentType);

        } else {

            stream.transferTo(outputStream);

        }

        return DownloadedFile.builder()
                .content(outputStream)
                .fileUri(fileUri)
                .contentType(parsedContentType)
                .build();
    }

    public static String getMediaPostfix(ContentType contentType) throws WrongContentTypeException {
        return switch (contentType){
            case PNG -> ".png";
            case JPEG -> ".jpeg";
            case WEBP -> ".webp";
            case WEBM -> ".webm";
            case MP4 -> ".mp4";
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
