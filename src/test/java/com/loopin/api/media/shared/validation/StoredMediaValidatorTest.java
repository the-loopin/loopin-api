package com.loopin.api.media.shared.validation;

import com.loopin.api.common.exception.InvalidMediaStateException;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.shared.policy.MediaUploadPolicy;
import com.loopin.api.media.storage.StoredObjectMetadata;
import com.loopin.api.media.support.MediaTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StoredMediaValidatorTest {

    private MediaUploadPolicy uploadPolicy;
    private StoredMediaValidator validator;

    @BeforeEach
    void setUp() {
        uploadPolicy = new MediaUploadPolicy();
        validator = new StoredMediaValidator(
            uploadPolicy
        );
    }

    @Test
    void validate_MatchingMetadata_Accepts() {
        MediaAsset media =
            MediaTestFixtures.pendingMedia();

        StoredObjectMetadata metadata =
            new StoredObjectMetadata(
                1_024L,
                "image/webp",
                "\"etag\""
            );

        assertDoesNotThrow(() ->
            validator.validate(media, metadata)
        );
    }

    @Test
    void validate_DifferentStoredSize_Rejects() {
        MediaAsset media =
            MediaTestFixtures.pendingMedia();

        StoredObjectMetadata metadata =
            new StoredObjectMetadata(
                2_048L,
                "image/webp",
                "\"etag\""
            );

        assertThrows(
            InvalidMediaStateException.class,
            () -> validator.validate(
                media,
                metadata
            )
        );
    }

    @Test
    void validate_DifferentStoredContentType_Rejects() {
        MediaAsset media =
            MediaTestFixtures.pendingMedia();

        StoredObjectMetadata metadata =
            new StoredObjectMetadata(
                1_024L,
                "image/png",
                "\"etag\""
            );

        assertThrows(
            InvalidMediaStateException.class,
            () -> validator.validate(
                media,
                metadata
            )
        );
    }

    @Test
    void validate_MissingStoredContentType_Rejects() {
        MediaAsset media =
            MediaTestFixtures.pendingMedia();

        StoredObjectMetadata metadata =
            new StoredObjectMetadata(
                1_024L,
                null,
                "\"etag\""
            );

        assertThrows(
            InvalidMediaStateException.class,
            () -> validator.validate(
                media,
                metadata
            )
        );
    }

    @Test
    void validate_UnsupportedStoredContentType_Rejects() {
        MediaAsset media =
            MediaTestFixtures.pendingMedia();

        StoredObjectMetadata metadata =
            new StoredObjectMetadata(
                1_024L,
                "application/octet-stream",
                "\"etag\""
            );

        assertThrows(
            InvalidMediaStateException.class,
            () -> validator.validate(
                media,
                metadata
            )
        );
    }
}
