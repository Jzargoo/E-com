package com.jzargo.media.storages.primary;

import com.jzargo.media.config.ApplicationPropertyStorage;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

@Slf4j
@Component
@Profile("s3")
public class PrimaryS3StorageInitializer {

    private final ApplicationPropertyStorage applicationPropertyStorage;
    private final S3Client s3Client;

    public PrimaryS3StorageInitializer(ApplicationPropertyStorage applicationPropertyStorage, S3Client s3Client) {
        this.applicationPropertyStorage = applicationPropertyStorage;
        this.s3Client = s3Client;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing Primary Storage...");
        initBuckets();
    }

    private void initBuckets() {
        String bucket = applicationPropertyStorage.getAws().getBucketName();

        initBucketIfNotExist(bucket);
    }

    private void initBucketIfNotExist(String bucket){
        log.debug("Initializing Primary Storage Bucket");

        try{

            s3Client.headBucket(HeadBucketRequest.builder()
                    .bucket(bucket)
                    .build()
            );

            log.debug("Head Bucket already exists");

        } catch (NoSuchBucketException e) {
            log.warn("Creating a bucket. {}", e.getMessage());

            s3Client.createBucket(
                    CreateBucketRequest.builder().bucket(bucket).build()
            );

            log.debug("Created a bucket. {}", e.getMessage());
        }

    }
}
