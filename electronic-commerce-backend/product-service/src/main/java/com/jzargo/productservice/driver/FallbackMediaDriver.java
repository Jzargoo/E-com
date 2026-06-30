package com.jzargo.productservice.driver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public interface FallbackMediaDriver {

    String saveFile(byte[] content) throws IOException;

    default List<String> saveFiles(byte[][] content) throws IOException {
        List<String> output = new ArrayList<>();
        try{
            for (byte[] file: content) {
                output.add(
                        saveFile(file)
                );
            }
        } catch (IOException e) {
            deleteFiles(output);
        }

        return output;
    }

    default List<byte[]> getContent(List<String> mediaIds) throws IOException {
        List<byte[]> files = new ArrayList<>();

        for (String mediaId: mediaIds) {
            files.add(
                    getFile(mediaId)
            );
        }

        return files;

    }

    byte[] getFile  (String mediaId) throws IOException;

    default void deleteFiles(List<String> fileNames) throws IOException{
        for (String fileName: fileNames) {
            deleteFile(fileName);
        }
    }

    void deleteFile(String fileName) throws IOException;

}
