package com.loopin.api.media.storage.s3;

import com.loopin.api.common.exception.MediaStorageException;
import com.loopin.api.common.exception.StoredObjectNotFoundException;
import com.loopin.api.media.storage.ObjectStorage;
import com.loopin.api.media.storage.PresignedUpload;
import com.loopin.api.media.storage.StorageProperties;
import com.loopin.api.media.storage.StoredObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
@Component
@RequiredArgsConstructor
public class S3ObjectStorage implements ObjectStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties properties;

    @Override
    public PresignedUpload createUploadUrl(
        String objectKey,
        String contentType,
        Duration expiration
    ) {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .contentType(contentType)
                .build();

            PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .putObjectRequest(putRequest)
                    .build();

            PresignedPutObjectRequest presigned =
                s3Presigner.presignPutObject(presignRequest);

            return new PresignedUpload(
                presigned.url().toURI(),
                Instant.now().plus(expiration)
            );
        } catch (Exception exception) {
            throw new MediaStorageException(
                "Could not generate upload URL",
                exception
            );
        }
    }

    @Override
    public StoredObjectMetadata getMetadata(String objectKey) {
        try {
            HeadObjectResponse response = s3Client.headObject(
                HeadObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .build()
            );

            return new StoredObjectMetadata(
                response.contentLength(),
                response.contentType(),
                response.eTag()
            );
        } catch (S3Exception exception) {
            if (isNotFound(exception)) {
                throw new StoredObjectNotFoundException();
            }

            throw new MediaStorageException(
                "Could not read stored object metadata",
                exception
            );
        } catch (SdkClientException exception) {
            throw new MediaStorageException(
                "Could not connect to object storage",
                exception
            );
        }
    }


    @Override
    public boolean exists(String objectKey) {
        try {
            getMetadata(objectKey);
            return true;
        } catch (StoredObjectNotFoundException exception) {
            return false;
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            s3Client.deleteObject(
                DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .build()
            );
        } catch (S3Exception exception) {
            if (isNotFound(exception)) {
                return;
            }

            throw new MediaStorageException(
                "Could not delete stored object",
                exception
            );
        } catch (SdkClientException exception) {
            throw new MediaStorageException(
                "Could not connect to object storage",
                exception
            );
        }
    }

    private boolean isNotFound(S3Exception exception) {
        if (exception.statusCode() == 404) {
            return true;
        }

        if (exception.awsErrorDetails() == null) {
            return false;
        }

        String errorCode = exception.awsErrorDetails().errorCode();

        return "NoSuchKey".equals(errorCode)
            || "NotFound".equals(errorCode);
    }
}
