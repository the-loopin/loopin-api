package com.loopin.api.media.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfiguration {

    @Bean
    public AwsCredentialsProvider storageCredentialsProvider(
        StorageProperties properties
    ) {
        return StaticCredentialsProvider.create(
            AwsBasicCredentials.create(
                properties.getAccessKey(),
                properties.getSecretKey()
            )
        );
    }

    @Bean
    public S3Configuration s3Configuration(StorageProperties properties) {
        return S3Configuration.builder()
            .pathStyleAccessEnabled(properties.isPathStyleAccess())
            .build();
    }

    @Bean(destroyMethod = "close")
    public S3Client s3Client(
        StorageProperties properties,
        AwsCredentialsProvider credentialsProvider,
        S3Configuration s3Configuration
    ) {
        return S3Client.builder()
            .endpointOverride(URI.create(properties.getEndpoint()))
            .region(Region.of(properties.getRegion()))
            .credentialsProvider(credentialsProvider)
            .serviceConfiguration(s3Configuration)
            .build();
    }

    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner(
        StorageProperties properties,
        AwsCredentialsProvider credentialsProvider,
        S3Configuration s3Configuration
    ) {
        return S3Presigner.builder()
            .endpointOverride(URI.create(properties.getPresignEndpoint()))
            .region(Region.of(properties.getRegion()))
            .credentialsProvider(credentialsProvider)
            .serviceConfiguration(s3Configuration)
            .build();
    }
}
