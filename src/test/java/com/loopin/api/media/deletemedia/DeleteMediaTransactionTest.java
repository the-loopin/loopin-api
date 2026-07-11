package com.loopin.api.media.deletemedia;

import com.loopin.api.common.exception.ForbiddenAccessException;
import com.loopin.api.common.exception.InvalidMediaStateException;
import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.enums.MediaStatus;
import com.loopin.api.media.repository.MediaAssetRepository;
import com.loopin.api.media.support.MediaTestFixtures;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeleteMediaTransactionTest {

    private MediaAssetRepository repository;
    private DeleteMediaTransaction transaction;

    @BeforeEach
    void setUp() {
        repository = mock(MediaAssetRepository.class);

        transaction =
            new DeleteMediaTransaction(repository);
    }

    @Test
    void markDeleted_PendingOwnedMedia_MarksDeleted() {
        User owner = MediaTestFixtures.owner();

        MediaAsset media =
            MediaTestFixtures.pendingMedia(owner);

        when(repository.findByPublicIdForUpdate(
            media.getPublicId()
        )).thenReturn(Optional.of(media));

        when(repository.save(media)).thenReturn(media);

        transaction.markDeleted(
            media.getPublicId(),
            owner.getId()
        );

        assertEquals(
            MediaStatus.DELETED,
            media.getStatus()
        );
        assertNotNull(media.getDeletedAt());

        verify(repository).save(media);
    }

    @Test
    void markDeleted_UploadedOwnedMedia_MarksDeleted() {
        User owner = MediaTestFixtures.owner();

        MediaAsset media =
            MediaTestFixtures.pendingMedia(owner);

        media.markUploaded(
            MediaTestFixtures.validMetadata()
        );

        when(repository.findByPublicIdForUpdate(
            media.getPublicId()
        )).thenReturn(Optional.of(media));

        transaction.markDeleted(
            media.getPublicId(),
            owner.getId()
        );

        assertEquals(
            MediaStatus.DELETED,
            media.getStatus()
        );
    }

    @Test
    void markDeleted_AlreadyDeleted_IsIdempotent() {
        User owner = MediaTestFixtures.owner();

        MediaAsset media =
            MediaTestFixtures.pendingMedia(owner);

        media.markDeleted();

        when(repository.findByPublicIdForUpdate(
            media.getPublicId()
        )).thenReturn(Optional.of(media));

        assertDoesNotThrow(() ->
            transaction.markDeleted(
                media.getPublicId(),
                owner.getId()
            )
        );

        verify(repository, never()).save(any());
    }

    @Test
    void markDeleted_AttachedMedia_Rejects() {
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

        assertThrows(
            InvalidMediaStateException.class,
            () -> transaction.markDeleted(
                media.getPublicId(),
                owner.getId()
            )
        );

        verify(repository, never()).save(any());
    }

    @Test
    void markDeleted_DifferentOwner_Rejects() {
        User owner = MediaTestFixtures.owner();
        User otherUser = MediaTestFixtures.otherUser();

        MediaAsset media =
            MediaTestFixtures.pendingMedia(owner);

        when(repository.findByPublicIdForUpdate(
            media.getPublicId()
        )).thenReturn(Optional.of(media));

        assertThrows(
            ForbiddenAccessException.class,
            () -> transaction.markDeleted(
                media.getPublicId(),
                otherUser.getId()
            )
        );

        verify(repository, never()).save(any());
    }

    @Test
    void markDeleted_MediaNotFound_ThrowsNotFound() {
        UUID mediaId = UUID.randomUUID();

        when(repository.findByPublicIdForUpdate(mediaId))
            .thenReturn(Optional.empty());

        assertThrows(
            ResourceNotFoundException.class,
            () -> transaction.markDeleted(
                mediaId,
                1L
            )
        );

        verify(repository, never()).save(any());
    }
}
