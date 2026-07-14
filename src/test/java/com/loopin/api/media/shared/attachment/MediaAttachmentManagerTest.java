package com.loopin.api.media.shared.attachment;

import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.repository.MediaAssetRepository;
import com.loopin.api.media.shared.cleanup.MediaObjectDeletionRequested;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static com.loopin.api.media.enums.MediaPurpose.EVENT_IMAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MediaAttachmentManagerTest {

    private MediaAssetRepository repository;
    private MediaAttachmentPolicy policy;
    private ApplicationEventPublisher eventPublisher;

    private MediaAttachmentManager manager;

    @BeforeEach
    void setUp() {
        repository =
            mock(MediaAssetRepository.class);

        policy =
            mock(MediaAttachmentPolicy.class);

        eventPublisher =
            mock(ApplicationEventPublisher.class);

        manager = new MediaAttachmentManager(
            repository,
            policy,
            eventPublisher
        );
    }

    @Test
    void attach_nullMediaId_returnsNullWithoutRepositoryAccess() {
        User owner = new User();

        MediaAsset result = manager.attach(
            null,
            owner,
            EVENT_IMAGE
        );

        assertNull(result);

        verifyNoInteractions(
            repository,
            policy,
            eventPublisher
        );
    }

    @Test
    void attach_validMedia_marksAttachedAndSaves() {
        UUID mediaId = UUID.randomUUID();

        User owner = new User();

        MediaAsset media =
            mock(MediaAsset.class);

        when(
            repository.findByPublicIdForUpdate(
                mediaId
            )
        ).thenReturn(
            Optional.of(media)
        );

        when(
            repository.save(media)
        ).thenReturn(media);

        MediaAsset result = manager.attach(
            mediaId,
            owner,
            EVENT_IMAGE
        );

        assertSame(media, result);

        verify(policy).requireNewAttachment(
            media,
            owner,
            EVENT_IMAGE
        );

        verify(media).markAttached();

        verify(repository).save(media);
    }

    @Test
    void attach_missingMedia_throwsNotFound() {
        UUID mediaId = UUID.randomUUID();

        when(
            repository.findByPublicIdForUpdate(
                mediaId
            )
        ).thenReturn(Optional.empty());

        assertThrows(
            ResourceNotFoundException.class,
            () -> manager.attach(
                mediaId,
                new User(),
                EVENT_IMAGE
            )
        );

        verify(
            repository,
            never()
        ).save(any());
    }

    @Test
    void replace_sameMediaId_preservesExistingAttachment() {
        UUID mediaId = UUID.randomUUID();

        User owner = new User();

        MediaAsset currentMedia =
            mock(MediaAsset.class);

        when(
            currentMedia.getPublicId()
        ).thenReturn(mediaId);

        when(
            repository.findByPublicIdForUpdate(
                mediaId
            )
        ).thenReturn(
            Optional.of(currentMedia)
        );

        MediaAsset result = manager.replace(
            currentMedia,
            mediaId,
            owner,
            EVENT_IMAGE
        );

        assertSame(currentMedia, result);

        verify(policy).requireExistingAttachment(
            currentMedia,
            EVENT_IMAGE
        );

        verify(
            policy,
            never()
        ).requireNewAttachment(
            any(),
            any(),
            any()
        );

        verify(
            repository,
            never()
        ).save(any());

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void replace_differentMedia_attachesNewAndDeletesOld() {
        UUID oldMediaId = UUID.randomUUID();
        UUID newMediaId = UUID.randomUUID();

        User owner = new User();

        MediaAsset oldMedia =
            mock(MediaAsset.class);

        MediaAsset newMedia =
            mock(MediaAsset.class);

        when(
            oldMedia.getPublicId()
        ).thenReturn(oldMediaId);

        when(
            oldMedia.getObjectKey()
        ).thenReturn(
            "events/old-image.webp"
        );

        when(
            repository.findByPublicIdForUpdate(
                newMediaId
            )
        ).thenReturn(
            Optional.of(newMedia)
        );

        when(
            repository.findByPublicIdForUpdate(
                oldMediaId
            )
        ).thenReturn(
            Optional.of(oldMedia)
        );

        when(
            repository.save(any(MediaAsset.class))
        ).thenAnswer(invocation ->
            invocation.getArgument(0)
        );

        MediaAsset result = manager.replace(
            oldMedia,
            newMediaId,
            owner,
            EVENT_IMAGE
        );

        assertSame(newMedia, result);

        verify(policy).requireNewAttachment(
            newMedia,
            owner,
            EVENT_IMAGE
        );

        verify(newMedia).markAttached();
        verify(repository).save(newMedia);

        verify(policy).requireExistingAttachment(
            oldMedia,
            EVENT_IMAGE
        );

        verify(oldMedia).markDetached();
        verify(oldMedia).markDeleted();
        verify(repository).save(oldMedia);

        ArgumentCaptor<MediaObjectDeletionRequested>
            eventCaptor =
            ArgumentCaptor.forClass(
                MediaObjectDeletionRequested.class
            );

        verify(eventPublisher).publishEvent(
            eventCaptor.capture()
        );

        assertEquals(
            oldMediaId,
            eventCaptor.getValue().mediaId()
        );

        assertEquals(
            "events/old-image.webp",
            eventCaptor.getValue().objectKey()
        );
    }

    @Test
    void replace_nullMediaId_removesCurrentMedia() {
        UUID oldMediaId = UUID.randomUUID();

        User owner = new User();

        MediaAsset oldMedia =
            mock(MediaAsset.class);

        when(
            oldMedia.getPublicId()
        ).thenReturn(oldMediaId);

        when(
            oldMedia.getObjectKey()
        ).thenReturn(
            "events/old-image.webp"
        );

        when(
            repository.findByPublicIdForUpdate(
                oldMediaId
            )
        ).thenReturn(
            Optional.of(oldMedia)
        );

        when(
            repository.save(oldMedia)
        ).thenReturn(oldMedia);

        MediaAsset result = manager.replace(
            oldMedia,
            null,
            owner,
            EVENT_IMAGE
        );

        assertNull(result);

        verify(oldMedia).markDetached();
        verify(oldMedia).markDeleted();

        verify(eventPublisher).publishEvent(
            any(MediaObjectDeletionRequested.class)
        );
    }

    @Test
    void remove_nullMedia_isIdempotent() {
        manager.remove(
            null,
            EVENT_IMAGE
        );

        verifyNoInteractions(
            repository,
            policy,
            eventPublisher
        );
    }

    @Test
    void remove_existingMedia_marksDeletedAndPublishesCleanupEvent() {
        UUID mediaId = UUID.randomUUID();

        MediaAsset media =
            mock(MediaAsset.class);

        when(
            media.getPublicId()
        ).thenReturn(mediaId);

        when(
            media.getObjectKey()
        ).thenReturn(
            "events/image.webp"
        );

        when(
            repository.findByPublicIdForUpdate(
                mediaId
            )
        ).thenReturn(
            Optional.of(media)
        );

        when(
            repository.save(media)
        ).thenReturn(media);

        manager.remove(
            media,
            EVENT_IMAGE
        );

        verify(policy).requireExistingAttachment(
            media,
            EVENT_IMAGE
        );

        verify(media).markDetached();
        verify(media).markDeleted();

        verify(repository).save(media);

        verify(eventPublisher).publishEvent(
            new MediaObjectDeletionRequested(
                mediaId,
                "events/image.webp"
            )
        );
    }
}
