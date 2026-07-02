package com.jzargo.media.storages.primary;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;

@Service
public class MediaPrimaryStorageServiceS3 {
    private final S3Client s3Client;

    public MediaPrimaryStorageServiceS3(S3Client s3Client) {
        this.s3Client = s3Client;
    }


}
