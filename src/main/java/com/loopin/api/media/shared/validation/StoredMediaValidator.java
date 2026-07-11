package com.loopin.api.media.shared.validation;

import com.loopin.api.common.exception.InvalidMediaStateException;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.shared.policy.MediaUploadPolicy;
import com.loopin.api.media.storage.StoredObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class StoredMediaValidator {

    private final MediaUploadPolicy uploadPolicy;

    public void validate(
        MediaAsset media,
        StoredObjectMetadata metadata
    ) {
        if (metadata.sizeBytes() != media.getDeclaredFileSize()) {
            throw new InvalidMediaStateException(
                "Stored file size does not match declared file size"
            );
        }

        if (metadata.contentType() == null
            || !metadata.contentType()
            .equalsIgnoreCase(media.getDeclaredContentType())) {
            throw new InvalidMediaStateException(
                "Stored content type does not match declared content type"
            );
        }

        uploadPolicy.validateRequest(
            media.getPurpose(),
            metadata.contentType().toLowerCase(Locale.ROOT),
            metadata.sizeBytes()
        );
    }
}
