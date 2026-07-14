package com.loopin.api.media.mapper;

import com.loopin.api.media.dto.response.MediaReferenceResponse;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class MediaReferenceMapper {

    private static final Duration DOWNLOAD_URL_EXPIRATION =
        Duration.ofHours(24);

    private final S3Presigner s3Presigner;
    private final StorageProperties properties;

    public MediaReferenceResponse toResponse(MediaAsset media) {
        if (media == null) {
            return null;
        }

        String contentType =
            media.getVerifiedContentType() != null
                ? media.getVerifiedContentType()
                : media.getDeclaredContentType();

        Long sizeBytes =
            media.getVerifiedFileSize() != null
                ? media.getVerifiedFileSize()
                : media.getDeclaredFileSize();

        GetObjectRequest getObjectRequest =
            GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(media.getObjectKey())
                .build();

        GetObjectPresignRequest presignRequest =
            GetObjectPresignRequest.builder()
                .signatureDuration(DOWNLOAD_URL_EXPIRATION)
                .getObjectRequest(getObjectRequest)
                .build();

        String url = s3Presigner
            .presignGetObject(presignRequest)
            .url()
            .toString();

        return new MediaReferenceResponse(
            media.getPublicId(),
            url,
            contentType,
            sizeBytes
        );
    }
}
