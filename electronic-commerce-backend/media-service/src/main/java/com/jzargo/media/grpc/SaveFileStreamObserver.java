package com.jzargo.media.grpc;

import com.jzargo.media.exceptions.CannotProcessException;
import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.helper.MediaHelper;
import com.jzargo.media.service.UploadSession;
import com.jzargo.protobuf.ContentType;
import com.jzargo.protobuf.MediaFile;
import com.jzargo.protobuf.VersionedURI;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class SaveFileStreamObserver implements StreamObserver<MediaFile> {

    private final UploadSession session;

    private final ContentType contentType;

    private final Consumer<Exception> doOnError;

    private final Consumer<VersionedURI> doOnSuccess;

    volatile boolean isFirst =  true;

    @Override
    public void onNext(MediaFile mediaFile) {

        if (isFirst) {
            log.info("Processing first chunk in save file stream observer");

            try {
                MediaHelper.checkContentType(mediaFile, contentType);
            } catch (WrongContentTypeException | IOException e) {
                doOnError.accept(e);
            }

            isFirst =  false;
        }

        try {
            session.process(mediaFile);
        } catch (CannotProcessException e) {
           doOnError.accept(e);
        }

    }

    @Override
    public void onError(Throwable throwable) {

        log.error(throwable.getMessage(), throwable);

        try {
            session.abort();
        } catch (CannotProcessException e) {
           log.info("cannot abort a session!");
        }

    }

    @Override
    public void onCompleted() {

        log.info("Completed save file stream observer");

        try {

            VersionedURI complete = session.complete();

            doOnSuccess.accept(complete);

        } catch (CannotProcessException e) {
           doOnError.accept(e);
        }
    }
}
