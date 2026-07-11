package com.loopin.api.media.entity;

import com.loopin.api.common.exception.InvalidMediaStateException;
import com.loopin.api.media.enums.MediaStatus;
import com.loopin.api.media.storage.StoredObjectMetadata;
import com.loopin.api.media.support.MediaTestFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaAssetTest {

    @Test
    void pending_CreatesPendingMedia() {
        MediaAsset media =
            MediaTestFixtures.pendingMedia();

        assertEquals(
            MediaStatus.PENDING_UPLOAD,
            media.getStatus()
        );

        assertNotNull(media.getPublicId());
    }

    @Test
    void markUploaded_PendingMedia_TransitionsToUploaded() {
        MediaAsset media =
            MediaTestFixtures.pendingMedia();

        StoredObjectMetadata metadata =
            MediaTestFixtures.validMetadata();

        media.markUploaded(metadata);

        assertEquals(
            MediaStatus.UPLOADED,
            media.getStatus()
        );

        assertEquals(
            metadata.contentType(),
            media.getVerifiedContentType()
        );

        assertEquals(
            metadata.sizeBytes(),
            media.getVerifiedFileSize()
        );
    }

    @Test
    void markAttached_UploadedMedia_TransitionsToAttached() {
        MediaAsset media =
            MediaTestFixtures.pendingMedia();

        media.markUploaded(
            MediaTestFixtures.validMetadata()
        );

        media.markAttached();

        assertEquals(
            MediaStatus.ATTACHED,
            media.getStatus()
        );
    }

    @Test
    void markAttached_PendingMedia_Rejects() {
        MediaAsset media =
            MediaTestFixtures.pendingMedia();

        assertThrows(
            InvalidMediaStateException.class,
            media::markAttached
        );
    }

    @Test
    void markDeleted_PendingMedia_TransitionsToDeleted() {
        MediaAsset media =
            MediaTestFixtures.pendingMedia();

        media.markDeleted();

        assertEquals(
            MediaStatus.DELETED,
            media.getStatus()
        );

        assertNotNull(media.getDeletedAt());
    }

    @Test
    void markDeleted_AttachedMedia_Rejects() {
        MediaAsset media =
            MediaTestFixtures.pendingMedia();

        media.markUploaded(
            MediaTestFixtures.validMetadata()
        );

        media.markAttached();

        assertThrows(
            InvalidMediaStateException.class,
            media::markDeleted
        );
    }
}
