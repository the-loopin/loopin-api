package com.loopin.api.media.completeupload;

import com.loopin.api.common.exception.ForbiddenAccessException;
import com.loopin.api.common.exception.InvalidMediaStateException;
import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.enums.MediaStatus;
import com.loopin.api.media.repository.MediaAssetRepository;
import com.loopin.api.media.storage.StoredObjectMetadata;
import com.loopin.api.media.support.MediaTestFixtures;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompleteMediaUploadTransactionTest {

    private MediaAssetRepository repository;
    private CompleteMediaUploadTransaction transaction;

    @BeforeEach
    void setUp() {
        repository = mock(MediaAssetRepository.class);

        transaction =
            new CompleteMediaUploadTransaction(repository);
    }

    @Test
    void complete_PendingOwnedMedia_MarksUploaded() {
        User owner = MediaTestFixtures.owner();

        MediaAsset media =
            MediaTestFixtures.pendingMedia(owner);

        StoredObjectMetadata metadata =
            MediaTestFixtures.validMetadata();

        when(repository.findByPublicIdForUpdate(
            media.getPublicId()
        )).thenReturn(Optional.of(media));

        when(repository.save(media)).thenReturn(media);

        MediaAsset result = transaction.complete(
            media.getPublicId(),
            owner.getId(),
            metadata
        );

        assertEquals(
            MediaStatus.UPLOADED,
            result.getStatus()
        );
        assertEquals(
            metadata.sizeBytes(),
            result.getVerifiedFileSize()
        );
        assertEquals(
            metadata.contentType(),
            result.getVerifiedContentType()
        );

        verify(repository).save(media);
    }

    @Test
    void complete_MediaNotFound_ThrowsNotFound() {
        UUID mediaId = UUID.randomUUID();

        when(repository.findByPublicIdForUpdate(mediaId))
            .thenReturn(Optional.empty());

        assertThrows(
            ResourceNotFoundException.class,
            () -> transaction.complete(
                mediaId,
                1L,
                MediaTestFixtures.validMetadata()
            )
        );

        verify(repository, never()).save(any());
    }

    @Test
    void complete_DifferentOwner_Rejects() {
        User owner = MediaTestFixtures.owner();
        User otherUser = MediaTestFixtures.otherUser();

        MediaAsset media =
            MediaTestFixtures.pendingMedia(owner);

        when(repository.findByPublicIdForUpdate(
            media.getPublicId()
        )).thenReturn(Optional.of(media));

        assertThrows(
            ForbiddenAccessException.class,
            () -> transaction.complete(
                media.getPublicId(),
                otherUser.getId(),
                MediaTestFixtures.validMetadata()
            )
        );

        verify(repository, never()).save(any());
    }

    @Test
    void complete_AlreadyUploaded_IsIdempotent() {
        User owner = MediaTestFixtures.owner();

        MediaAsset media =
            MediaTestFixtures.pendingMedia(owner);

        media.markUploaded(
            MediaTestFixtures.validMetadata()
        );

        when(repository.findByPublicIdForUpdate(
            media.getPublicId()
        )).thenReturn(Optional.of(media));

        MediaAsset result = transaction.complete(
            media.getPublicId(),
            owner.getId(),
            MediaTestFixtures.validMetadata()
        );

        assertEquals(
            MediaStatus.UPLOADED,
            result.getStatus()
        );

        verify(repository, never()).save(any());
    }

    @Test
    void complete_AttachedMedia_IsIdempotent() {
        User owner = MediaTestFixtures.owner();

        MediaAsset media =
            MediaTestFixtures.pendingMedia(owner);

        media.markUploaded(
            MediaTestFixtures.validMetadata()
        );
        media.markAttached();

        when(repository.findByPublicIdForUpdate(
            media.getPublicId()
        )).thenReturn(Optional.of(media));

        MediaAsset result = transaction.complete(
            media.getPublicId(),
            owner.getId(),
            MediaTestFixtures.validMetadata()
        );

        assertEquals(
            MediaStatus.ATTACHED,
            result.getStatus()
        );

        verify(repository, never()).save(any());
    }

    @Test
    void complete_DeletedMedia_Rejects() {
        User owner = MediaTestFixtures.owner();

        MediaAsset media =
            MediaTestFixtures.pendingMedia(owner);

        media.markDeleted();

        when(repository.findByPublicIdForUpdate(
            media.getPublicId()
        )).thenReturn(Optional.of(media));

        assertThrows(
            InvalidMediaStateException.class,
            () -> transaction.complete(
                media.getPublicId(),
                owner.getId(),
                MediaTestFixtures.validMetadata()
            )
        );

        verify(repository, never()).save(any());
    }
}
