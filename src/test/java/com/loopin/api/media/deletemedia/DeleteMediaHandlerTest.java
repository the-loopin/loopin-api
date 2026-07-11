package com.loopin.api.media.deletemedia;

import com.loopin.api.common.exception.ForbiddenAccessException;
import com.loopin.api.common.exception.InvalidMediaStateException;
import com.loopin.api.common.exception.MediaStorageException;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.shared.access.MediaAccessPolicy;
import com.loopin.api.media.shared.finder.MediaFinder;
import com.loopin.api.media.storage.ObjectStorage;
import com.loopin.api.media.support.MediaTestFixtures;
import com.loopin.api.users.api.UserLookup;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeleteMediaHandlerTest {

    private UserLookup userLookup;
    private MediaFinder mediaFinder;
    private MediaAccessPolicy accessPolicy;
    private ObjectStorage objectStorage;
    private DeleteMediaTransaction transaction;

    private DeleteMediaHandler handler;

    @BeforeEach
    void setUp() {
        userLookup = mock(UserLookup.class);
        mediaFinder = mock(MediaFinder.class);
        accessPolicy = mock(MediaAccessPolicy.class);
        objectStorage = mock(ObjectStorage.class);
        transaction = mock(DeleteMediaTransaction.class);

        handler = new DeleteMediaHandler(
            userLookup,
            mediaFinder,
            accessPolicy,
            objectStorage,
            transaction
        );
    }

    @Test
    void handle_PendingMedia_DeletesObjectAndMarksDeleted() {
        User owner = MediaTestFixtures.owner();
        MediaAsset media =
            MediaTestFixtures.pendingMedia(owner);

        prepareOwnerAndMedia(owner, media);

        handler.handle(
            new DeleteMediaCommand(
                media.getPublicId(),
                owner.getEmail()
            )
        );

        verify(objectStorage)
            .delete(media.getObjectKey());

        verify(transaction).markDeleted(
            media.getPublicId(),
            owner.getId()
        );
    }

    @Test
    void handle_UploadedMedia_DeletesObjectAndMarksDeleted() {
        User owner = MediaTestFixtures.owner();

        MediaAsset media =
            MediaTestFixtures.pendingMedia(owner);

        media.markUploaded(
            MediaTestFixtures.validMetadata()
        );

        prepareOwnerAndMedia(owner, media);

        handler.handle(
            new DeleteMediaCommand(
                media.getPublicId(),
                owner.getEmail()
            )
        );

        verify(objectStorage)
            .delete(media.getObjectKey());

        verify(transaction).markDeleted(
            media.getPublicId(),
            owner.getId()
        );
    }

    @Test
    void handle_AlreadyDeleted_IsNoOp() {
        User owner = MediaTestFixtures.owner();

        MediaAsset media =
            MediaTestFixtures.pendingMedia(owner);

        media.markDeleted();

        prepareOwnerAndMedia(owner, media);

        assertDoesNotThrow(() ->
            handler.handle(
                new DeleteMediaCommand(
                    media.getPublicId(),
                    owner.getEmail()
                )
            )
        );

        verify(objectStorage, never())
            .delete(anyString());

        verify(transaction, never())
            .markDeleted(any(), any());
    }

    @Test
    void handle_AttachedMedia_RejectsBeforeStorageCall() {
        User owner = MediaTestFixtures.owner();

        MediaAsset media =
            MediaTestFixtures.pendingMedia(owner);

        media.markUploaded(
            MediaTestFixtures.validMetadata()
        );
        media.markAttached();

        prepareOwnerAndMedia(owner, media);

        assertThrows(
            InvalidMediaStateException.class,
            () -> handler.handle(
                new DeleteMediaCommand(
                    media.getPublicId(),
                    owner.getEmail()
                )
            )
        );

        verify(objectStorage, never())
            .delete(anyString());

        verify(transaction, never())
            .markDeleted(any(), any());
    }

    @Test
    void handle_DifferentOwner_RejectsBeforeStorageCall() {
        User owner = MediaTestFixtures.owner();
        User otherUser = MediaTestFixtures.otherUser();

        MediaAsset media =
            MediaTestFixtures.pendingMedia(owner);

        when(userLookup.findByEmail(
            otherUser.getEmail()
        )).thenReturn(otherUser);

        when(mediaFinder.findByPublicId(
            media.getPublicId()
        )).thenReturn(media);

        doThrow(new ForbiddenAccessException(
            "You do not own this media asset"
        )).when(accessPolicy).requireOwner(
            media,
            otherUser
        );

        assertThrows(
            ForbiddenAccessException.class,
            () -> handler.handle(
                new DeleteMediaCommand(
                    media.getPublicId(),
                    otherUser.getEmail()
                )
            )
        );

        verify(objectStorage, never())
            .delete(anyString());

        verify(transaction, never())
            .markDeleted(any(), any());
    }

    @Test
    void handle_StorageFailure_DoesNotMarkDatabaseDeleted() {
        User owner = MediaTestFixtures.owner();

        MediaAsset media =
            MediaTestFixtures.pendingMedia(owner);

        prepareOwnerAndMedia(owner, media);

        doThrow(new MediaStorageException(
            "Storage unavailable"
        )).when(objectStorage)
            .delete(media.getObjectKey());

        assertThrows(
            MediaStorageException.class,
            () -> handler.handle(
                new DeleteMediaCommand(
                    media.getPublicId(),
                    owner.getEmail()
                )
            )
        );

        verify(transaction, never())
            .markDeleted(any(), any());
    }

    private void prepareOwnerAndMedia(
        User owner,
        MediaAsset media
    ) {
        when(userLookup.findByEmail(owner.getEmail()))
            .thenReturn(owner);

        when(mediaFinder.findByPublicId(
            media.getPublicId()
        )).thenReturn(media);
    }
}
