package com.loopin.api.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.net.URI;

public abstract class AbstractMinioIntegrationTest extends AbstractIntegrationTest {

    private static final String MEDIA_BUCKET = "loopin-media-test";
    private static final MinIOContainer minio = new MinIOContainer(
            "minio/minio:RELEASE.2023-09-04T19-57-37Z"
    )
            .withUserName("test-access-key")
            .withPassword("test-secret-key");

    static {
        minio.start();
        createMediaBucket();
    }

    @DynamicPropertySource
    static void configureMinioProperties(DynamicPropertyRegistry registry) {
        registry.add("loopin.storage.endpoint", minio::getS3URL);
        registry.add("loopin.storage.presign-endpoint", minio::getS3URL);
        registry.add("loopin.storage.region", () -> "us-east-1");
        registry.add("loopin.storage.bucket", () -> MEDIA_BUCKET);
        registry.add("loopin.storage.access-key", minio::getUserName);
        registry.add("loopin.storage.secret-key", minio::getPassword);
        registry.add("loopin.storage.path-style-access", () -> "true");
    }

    private static void createMediaBucket() {
        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(minio.getUserName(), minio.getPassword())
        );
        try (S3Client s3Client = S3Client.builder()
                .endpointOverride(URI.create(minio.getS3URL()))
                .region(Region.US_EAST_1)
                .credentialsProvider(credentials)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build()) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(MEDIA_BUCKET).build());
        }
    }
}
