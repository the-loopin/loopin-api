package com.loopin.api.media.repository;

import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.enums.MediaPurpose;
import com.loopin.api.media.enums.MediaStatus;
import com.loopin.api.support.AbstractIntegrationTest;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaAssetRepositoryIntegrationTest
    extends AbstractIntegrationTest {

    @Autowired
    private MediaAssetRepository mediaRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = userRepository.saveAndFlush(
            new User(
                "media-repository-owner@example.test",
                "Media Repository Owner",
                null
            )
        );
    }

    @Test
    void saveAndFindByPublicId_PersistsMedia() {
        MediaAsset media = newPendingMedia(
            UUID.randomUUID(),
            "media/repository/first"
        );

        mediaRepository.saveAndFlush(media);

        MediaAsset result = mediaRepository
            .findByPublicId(media.getPublicId())
            .orElseThrow();

        assertEquals(
            media.getPublicId(),
            result.getPublicId()
        );

        assertEquals(
            owner.getId(),
            result.getOwner().getId()
        );

        assertEquals(
            MediaStatus.PENDING_UPLOAD,
            result.getStatus()
        );
    }

    @Test
    void findByPublicIdForUpdate_ReturnsMedia() {
        MediaAsset media = newPendingMedia(
            UUID.randomUUID(),
            "media/repository/locked"
        );

        mediaRepository.saveAndFlush(media);

        MediaAsset result = mediaRepository
            .findByPublicIdForUpdate(
                media.getPublicId()
            )
            .orElseThrow();

        assertEquals(
            media.getPublicId(),
            result.getPublicId()
        );
    }

    @Test
    void duplicateObjectKey_IsRejected() {
        String objectKey =
            "media/repository/duplicate-key";

        MediaAsset first = newPendingMedia(
            UUID.randomUUID(),
            objectKey
        );

        MediaAsset second = newPendingMedia(
            UUID.randomUUID(),
            objectKey
        );

        mediaRepository.saveAndFlush(first);

        assertThrows(
            DataIntegrityViolationException.class,
            () -> mediaRepository.saveAndFlush(second)
        );
    }

    @Test
    void duplicatePublicId_IsRejected() {
        UUID publicId = UUID.randomUUID();

        MediaAsset first = newPendingMedia(
            publicId,
            "media/repository/first-public-id"
        );

        MediaAsset second = newPendingMedia(
            publicId,
            "media/repository/second-public-id"
        );

        mediaRepository.saveAndFlush(first);

        assertThrows(
            DataIntegrityViolationException.class,
            () -> mediaRepository.saveAndFlush(second)
        );
    }

    @Test
    void deletedMedia_RemainsFindableForIdempotentDelete() {
        MediaAsset media = newPendingMedia(
            UUID.randomUUID(),
            "media/repository/deleted"
        );

        mediaRepository.saveAndFlush(media);

        media.markDeleted();
        mediaRepository.saveAndFlush(media);

        MediaAsset result = mediaRepository
            .findByPublicId(media.getPublicId())
            .orElseThrow();

        assertEquals(
            MediaStatus.DELETED,
            result.getStatus()
        );

        assertTrue(result.getDeletedAt() != null);
    }

    private MediaAsset newPendingMedia(
        UUID publicId,
        String objectKey
    ) {
        return MediaAsset.pending(
            publicId,
            owner,
            objectKey,
            "event.webp",
            "image/webp",
            1_024L,
            MediaPurpose.EVENT_IMAGE
        );
    }
}
