package com.jzargo.media.service;

import com.jzargo.media.config.ApplicationPropertyStorage;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;

@Slf4j
@Component
public class TempFileBufferFactory {

    private final Path tempDirectory;
    private final Long threshold;

    public TempFileBufferFactory(ApplicationPropertyStorage propertyStorage) {
        this.threshold = propertyStorage.getAws().getSmartStreamProperties().getThreshold();

        try {
            this.tempDirectory = Files.createTempDirectory(
                    propertyStorage.getNativeStorageOptions().getTempDirectory()
            );
        } catch (IOException e) {
            log.error("Error creating temp directory", e);
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void destroy() {
        try (
                var walker = Files.walk(tempDirectory)
        ){

            walker.sorted(Comparator.reverseOrder())
                    .forEach(file -> {

                        try {

                            Files.delete(file);

                        } catch (IOException e) {

                            log.error("Error deleting temp file {}", file, e);

                            throw new RuntimeException(e);

                        }

                    });

            Files.deleteIfExists(
                    tempDirectory
            );

        } catch (IOException e) {

            log.error("Error deleting temp directory", e);

            throw new RuntimeException(e);

        }
    }


    public TempFileBuffer createBuffer() {
        try {
            return new TempFileBuffer(tempDirectory.getFileName().toString(), threshold);
        } catch (IOException e) {
            log.error("Error creating temp buffer", e);
            throw new RuntimeException(e);
        }
    }



    public static class TempFileBuffer {
        private final Path tempFile;
        private final OutputStream outputStream;
        private long size = 0;
        private final long threshold;

        public TempFileBuffer(String directory, long threshold) throws IOException {
            this.tempFile = Files.createTempFile(
                    directory, UUID.randomUUID().toString()
            );

            this.outputStream = Files.newOutputStream(tempFile);

            this.threshold = threshold;
        }

        public boolean addChunkRespectToThreshold(byte[] chunk) throws IOException {
            outputStream.write(chunk);

            size += chunk.length;

            return size >= threshold;
        }

        public InputStream getChunk() throws IOException {
            return Files.newInputStream(tempFile);
        }

        public void close() throws IOException {
            outputStream.close();

            Files.deleteIfExists(tempFile);
        }

        public Long getSize() {
            return size;
        }
    }

}
