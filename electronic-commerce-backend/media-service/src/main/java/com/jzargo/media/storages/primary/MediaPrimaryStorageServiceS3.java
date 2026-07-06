package com.jzargo.media.storages.primary;

import com.jzargo.media.config.ApplicationPropertyStorage;
import com.jzargo.media.exceptions.CannotDownloadFileException;
import com.jzargo.media.helper.MediaHelper;
import com.jzargo.media.model.DownloadedFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;

@Slf4j
@Service
public class MediaPrimaryStorageServiceS3 implements MediaPrimaryStorageService {
    private final S3Client s3Client;

    private final String bucketName;

    public MediaPrimaryStorageServiceS3(S3Client s3Client, ApplicationPropertyStorage aps) {
        this.s3Client = s3Client;
        this.bucketName = aps.getAws().getBucketName();
    }


    @Override
    public void deleteFile(String fileUri) {

    }

    @Override
    public DownloadedFile downloadFile(String fileUri) throws CannotDownloadFileException {

        var request = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(fileUri)
                .build();

        try {
            HeadObjectResponse headObjectResponse = s3Client.headObject(request);

            var getRequest = GetObjectRequest
                    .builder()
                    .bucket(bucketName)
                    .key(fileUri)
                    .build();


            ResponseInputStream<GetObjectResponse> object = s3Client.getObject(getRequest);


            String contentType = headObjectResponse.contentType();

            return MediaHelper.createFileRepresentation(object, contentType, fileUri);


        } catch (NoSuchBucketException e) {
            log.warn("The bucket was not initialized, it might be deleted. Creating a bucket with name {}", bucketName);

            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());

            log.debug("After creating a bucket it must be empty!");

            throw new CannotDownloadFileException("The bucket was not initialized, it might be deleted!");

        } catch (IOException e) {
            log.error("Error during downloading file at {}, skipping this task", fileUri, e);

            throw new CannotDownloadFileException("IO exception occurred in reading bytes from input stream");
        } catch (NoSuchKeyException e) {
            log.info("There was a try to download a file that was deleted" +
                    ", expecting that a deleter will publish sync event and will be available");

            throw new CannotDownloadFileException("The file was deleted!");
        }
    }

    @Override
    public void uploadFile() {

    }
}
