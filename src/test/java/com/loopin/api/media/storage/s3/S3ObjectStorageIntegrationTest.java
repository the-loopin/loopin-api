package com.loopin.api.media.storage.s3;

import com.loopin.api.common.exception.StoredObjectNotFoundException;
import com.loopin.api.media.storage.PresignedUpload;
import com.loopin.api.media.storage.StorageProperties;
import com.loopin.api.media.storage.StoredObjectMetadata;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class S3ObjectStorageIntegrationTest {

    private static final String BUCKET =
        "loopin-media-test";

    @Container
    static final MinIOContainer minio =
        new MinIOContainer(
            "minio/minio:"
                + "RELEASE.2023-09-04T19-57-37Z"
        )
            .withUserName("test-access-key")
            .withPassword("test-secret-key");

    private static S3Client s3Client;
    private static S3Presigner s3Presigner;
    private static S3ObjectStorage objectStorage;
    private static HttpClient httpClient;

    @BeforeAll
    static void setUp() {
        StaticCredentialsProvider credentials =
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    minio.getUserName(),
                    minio.getPassword()
                )
            );

        S3Configuration s3Configuration =
            S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();

        s3Client = S3Client.builder()
            .endpointOverride(
                URI.create(minio.getS3URL())
            )
            .region(Region.US_EAST_1)
            .credentialsProvider(credentials)
            .serviceConfiguration(s3Configuration)
            .build();

        s3Presigner = S3Presigner.builder()
            .endpointOverride(
                URI.create(minio.getS3URL())
            )
            .region(Region.US_EAST_1)
            .credentialsProvider(credentials)
            .serviceConfiguration(s3Configuration)
            .build();

        s3Client.createBucket(
            CreateBucketRequest.builder()
                .bucket(BUCKET)
                .build()
        );

        StorageProperties properties =
            new StorageProperties();

        properties.setEndpoint(minio.getS3URL());
        properties.setPresignEndpoint(minio.getS3URL());
        properties.setRegion("us-east-1");
        properties.setBucket(BUCKET);
        properties.setAccessKey(minio.getUserName());
        properties.setSecretKey(minio.getPassword());
        properties.setPathStyleAccess(true);
        properties.setUploadUrlTtl(
            Duration.ofMinutes(5)
        );

        objectStorage = new S3ObjectStorage(
            s3Client,
            s3Presigner,
            properties
        );

        httpClient = HttpClient.newHttpClient();
    }

    @AfterAll
    static void tearDown() {
        if (s3Presigner != null) {
            s3Presigner.close();
        }

        if (s3Client != null) {
            s3Client.close();
        }
    }

    @Test
    void presignedUploadMetadataAndDelete_WorkEndToEnd()
        throws Exception {

        String objectKey =
            "media/test/event-image/"
                + UUID.randomUUID();

        byte[] content =
            "fake-webp-test-content"
                .getBytes(StandardCharsets.UTF_8);

        PresignedUpload upload =
            objectStorage.createUploadUrl(
                objectKey,
                "image/webp",
                Duration.ofMinutes(5)
            );

        assertNotNull(upload.uploadUrl());
        assertNotNull(upload.expiresAt());

        HttpRequest putRequest =
            HttpRequest.newBuilder()
                .uri(upload.uploadUrl())
                .header(
                    "Content-Type",
                    "image/webp"
                )
                .PUT(
                    HttpRequest.BodyPublishers
                        .ofByteArray(content)
                )
                .build();

        HttpResponse<Void> putResponse =
            httpClient.send(
                putRequest,
                HttpResponse.BodyHandlers
                    .discarding()
            );

        assertTrue(
            putResponse.statusCode() >= 200
                && putResponse.statusCode() < 300,
            "Unexpected PUT status: "
                + putResponse.statusCode()
        );

        assertTrue(
            objectStorage.exists(objectKey)
        );

        StoredObjectMetadata metadata =
            objectStorage.getMetadata(objectKey);

        assertEquals(
            content.length,
            metadata.sizeBytes()
        );

        assertEquals(
            "image/webp",
            metadata.contentType()
        );

        assertNotNull(metadata.eTag());

        objectStorage.delete(objectKey);

        assertFalse(
            objectStorage.exists(objectKey)
        );
    }

    @Test
    void getMetadata_MissingObject_ThrowsExpectedException() {
        String missingKey =
            "media/test/missing/"
                + UUID.randomUUID();

        assertThrows(
            StoredObjectNotFoundException.class,
            () -> objectStorage.getMetadata(
                missingKey
            )
        );
    }

    @Test
    void delete_MissingObject_IsIdempotent() {
        String missingKey =
            "media/test/already-missing/"
                + UUID.randomUUID();

        assertDoesNotThrow(
            () -> objectStorage.delete(missingKey)
        );

        assertDoesNotThrow(
            () -> objectStorage.delete(missingKey)
        );
    }

    @Test
    void presignedUpload_WithDifferentContentType_IsRejected()
        throws Exception {

        String objectKey =
            "media/test/wrong-content-type/"
                + UUID.randomUUID();

        PresignedUpload upload =
            objectStorage.createUploadUrl(
                objectKey,
                "image/webp",
                Duration.ofMinutes(5)
            );

        HttpRequest putRequest =
            HttpRequest.newBuilder()
                .uri(upload.uploadUrl())
                .header(
                    "Content-Type",
                    "image/png"
                )
                .PUT(
                    HttpRequest.BodyPublishers
                        .ofByteArray(
                            "test".getBytes(
                                StandardCharsets.UTF_8
                            )
                        )
                )
                .build();

        HttpResponse<Void> response =
            httpClient.send(
                putRequest,
                HttpResponse.BodyHandlers
                    .discarding()
            );

        assertTrue(
            response.statusCode() >= 400,
            "Expected rejected signature, but status was "
                + response.statusCode()
        );

        assertFalse(
            objectStorage.exists(objectKey)
        );
    }
}
