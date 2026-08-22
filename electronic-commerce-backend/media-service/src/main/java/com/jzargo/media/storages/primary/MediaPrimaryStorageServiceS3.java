package com.jzargo.media.storages.primary;

import com.jzargo.media.config.ApplicationPropertyStorage;
import com.jzargo.media.exceptions.CannotDownloadFileException;
import com.jzargo.media.exceptions.CannotProcessException;
import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.helper.MediaHelper;
import com.jzargo.media.model.DownloadedFile;
import com.jzargo.protobuf.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MediaPrimaryStorageServiceS3 implements MediaPrimaryStorageService {
    private final S3Client s3Client;

    private final String versionAttr;

    private final String bucketName;

    public MediaPrimaryStorageServiceS3(S3Client s3Client, ApplicationPropertyStorage aps) {
        this.s3Client = s3Client;
        this.bucketName = aps.getAws().getBucket();
        this.versionAttr = aps.getAws().getVersionAttribute();
    }


    @Override
    public void deleteFile(String fileUri) throws CannotProcessException {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest
                .builder()
                .bucket(bucketName)
                .key(fileUri)
                .build();
        try {
            s3Client.deleteObject(deleteObjectRequest);
        } catch (SdkClientException e) {
            throw new CannotProcessException();
        }

    }

    @Override
    public DownloadedFile downloadFile(String fileUri) throws CannotDownloadFileException {

        try {

            var getRequest = GetObjectRequest
                    .builder()
                    .bucket(bucketName)
                    .key(fileUri)
                    .build();


            ResponseInputStream<GetObjectResponse> object = s3Client.getObject(getRequest);


            String contentType = object.response().contentType();

            return MediaHelper.createFileRepresentation(
                    object,
                    object.response().contentLength(),
                    contentType,
                    fileUri
            );

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
        } catch (CannotProcessException e) {
            throw new RuntimeException(e);
        } catch (WrongContentTypeException e) {
            log.error("The function cannot treat a file because it provide incorrect ");
            throw new RuntimeException(e);
        }

    }

    @Override
    public String uploadPartOfFile(String uploadId, String key, InputStream is, Integer partNumber, Long length) {

        UploadPartRequest build = UploadPartRequest.builder()
                .bucket(bucketName)
                .key(key)
                .partNumber(partNumber)
                .uploadId(uploadId)
                .contentLength(length)
                .build();

        RequestBody body = RequestBody.fromInputStream(is, length);

        UploadPartResponse uploadPartResponse = s3Client.uploadPart(build, body);

        return uploadPartResponse.eTag();
    }

    @Override
    public String startUploadingFile(ContentType contentType, String key, String versionId) throws WrongContentTypeException {

        var versionValue = versionId == null?
                UUID.randomUUID().toString() :
                versionId;

        CreateMultipartUploadRequest request =
                CreateMultipartUploadRequest.builder()
                        .bucket(bucketName)
                        .expires(
                                Instant.now().plus(1, TimeUnit.DAYS.toChronoUnit())
                        )
                        .metadata(
                                Map.of(
                                        versionAttr, versionValue
                                )
                        )
                        .key(key)
                        .contentType(MediaHelper.parseToMime(contentType))
                        .build();

        CreateMultipartUploadResponse multipartUpload
                = s3Client.createMultipartUpload(request);

        return multipartUpload.uploadId();
    }

    @Override
    public CompleteMultipartUploadResponse finishFileUploading(String key, String uploadId, List<String> tags) throws CannotProcessException {

        List<CompletedPart> completedParts = new ArrayList<>();

        for (int i =  0; i < tags.size(); i++) {

            log.info("Constructing a part with tag {} and number {}",  tags.get(i), i);

            completedParts.add(
                    CompletedPart.builder()
                            .eTag(tags.get(i))
                            .partNumber(i + 1)
                            .build()
            );
        }

        CompleteMultipartUploadRequest request = CompleteMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(key)
                .uploadId(uploadId)
                .multipartUpload(
                        CompletedMultipartUpload.builder()
                                .parts(completedParts)
                                .build()
                )
                .build();

        try {
             return s3Client.completeMultipartUpload(request);

        } catch (SdkClientException e) {
            throw new CannotProcessException();
        } catch (Exception e) {
            log.error("Exception occurred in finishFileUploading", e);
            throw new CannotProcessException();
        }
    }

    @Override
    public void abortMultipartFile(String key, String uploadId) {
        AbortMultipartUploadRequest build = AbortMultipartUploadRequest
                .builder()
                .bucket(bucketName)
                .key(key)
                .uploadId(uploadId)
                .build();

        s3Client.abortMultipartUpload(build);
    }

    @Override
    public String uploadFullFile(DownloadedFile file, Optional<String> ttl) {

        var versionValue = file.getVersionId() == null?
                UUID.randomUUID().toString() :
                file.getVersionId();

        PutObjectRequest.Builder request = PutObjectRequest.builder()
                .bucket(bucketName)
                .metadata(
                        Map.of(
                                versionAttr, versionValue
                        )
                )
                .key(file.getFileUri());

        ttl.ifPresent((value) -> {
            Tag tagTtl = Tag.builder()
                    .key("ttl")
                    .value(value)
                    .build();

            request.tagging(
                    Tagging.builder()
                            .tagSet(tagTtl)
                            .build()
            );
        });


        try ( InputStream in = file.getContent() ) {

            s3Client.putObject(
                    request.build(),
                    RequestBody.fromInputStream(in, file.getContentLength())
            );

        } catch (IOException e) {

            log.error("Cannot close an input stream", e);

        }

        return versionValue;
    }

    @Override
    public Boolean existsByVersion(String uri, String version) throws NoSuchKeyException{
        HeadObjectRequest build = HeadObjectRequest.builder()
                .key(uri)
                .bucket(bucketName)
                .build();


        HeadObjectResponse headObjectResponse = s3Client.headObject(build);

        return headObjectResponse.versionId().equals(version);
    }
}