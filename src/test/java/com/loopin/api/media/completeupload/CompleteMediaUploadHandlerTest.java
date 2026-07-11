package com.loopin.api.media.completeupload;

import com.loopin.api.common.exception.ForbiddenAccessException;
import com.loopin.api.common.exception.InvalidMediaStateException;
import com.loopin.api.common.exception.StoredObjectNotFoundException;
import com.loopin.api.media.dto.response.MediaCompletionResponse;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.enums.MediaStatus;
import com.loopin.api.media.shared.access.MediaAccessPolicy;
import com.loopin.api.media.shared.finder.MediaFinder;
import com.loopin.api.media.shared.validation.StoredMediaValidator;
import com.loopin.api.media.storage.ObjectStorage;
import com.loopin.api.media.storage.StoredObjectMetadata;
import com.loopin.api.media.support.MediaTestFixtures;
import com.loopin.api.users.api.UserLookup;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompleteMediaUploadHandlerTest {

    private UserLookup userLookup;
    private MediaFinder mediaFinder;
    private MediaAccessPolicy accessPolicy;
    private ObjectStorage objectStorage;
    private StoredMediaValidator validator;
    private CompleteMediaUploadTransaction transaction;

    private CompleteMediaUploadHandler handler;

    @BeforeEach
    void setUp() {
        userLookup = mock(UserLookup.class);
        mediaFinder = mock(MediaFinder.class);
        accessPolicy = mock(MediaAccessPolicy.class);
        objectStorage = mock(ObjectStorage.class);
        validator = mock(StoredMediaValidator.class);
        transaction = mock(CompleteMediaUploadTransaction.class);

        handler = new CompleteMediaUploadHandler(
            userLookup,
            mediaFinder,
            accessPolicy,
            objectStorage,
            validator,
            transaction
        );
    }

    @Test
    void handle_PendingOwnedMedia_CompletesSuccessfully() {
        User owner = MediaTestFixtures.owner();
        MediaAsset media =
            MediaTestFixtures.pendingMedia(owner);

        StoredObjectMetadata metadata =
            MediaTestFixtures.validMetadata();

        prepareOwnerAndMedia(owner, media);

        when(objectStorage.getMetadata(
            media.getObjectKey()
        )).thenReturn(metadata);

        when(transaction.complete(
            media.getPublicId(),
            owner.getId(),
            metadata
        )).thenAnswer(invocation -> {
            media.markUploaded(metadata);
            return media;
        });

        MediaCompletionResponse response = handler.handle(
            new CompleteMediaUploadCommand(
                media.getPublicId(),
                owner.getEmail()
            )
        );

        assertEquals(
            MediaStatus.UPLOADED,
            response.status()
        );

        verify(accessPolicy).requireOwner(media, owner);
        verify(validator).validate(media, metadata);

        verify(transaction).complete(
            media.getPublicId(),
            owner.getId(),
            metadata
        );
    }

    @Test
    void handle_MissingStoredObject_DoesNotComplete() {
        User owner = MediaTestFixtures.owner();
        MediaAsset media =
            MediaTestFixtures.pendingMedia(owner);

        prepareOwnerAndMedia(owner, media);

        when(objectStorage.getMetadata(
            media.getObjectKey()
        )).thenThrow(
            new StoredObjectNotFoundException()
        );

        assertThrows(
            StoredObjectNotFoundException.class,
            () -> handler.handle(
                new CompleteMediaUploadCommand(
                    media.getPublicId(),
                    owner.getEmail()
                )
            )
        );

        verify(validator, never())
            .validate(any(), any());

        verify(transaction, never())
            .complete(any(), any(), any());
    }

    @Test
    void handle_MetadataMismatch_DoesNotComplete() {
        User owner = MediaTestFixtures.owner();
        MediaAsset media =
            MediaTestFixtures.pendingMedia(owner);

        StoredObjectMetadata invalidMetadata =
            new StoredObjectMetadata(
                2_048L,
                "image/webp",
                "\"etag\""
            );

        prepareOwnerAndMedia(owner, media);

        when(objectStorage.getMetadata(anyString()))
            .thenReturn(invalidMetadata);

        doThrow(new InvalidMediaStateException(
            "Stored file size does not match"
        )).when(validator).validate(
            media,
            invalidMetadata
        );

        assertThrows(
            InvalidMediaStateException.class,
            () -> handler.handle(
                new CompleteMediaUploadCommand(
                    media.getPublicId(),
                    owner.getEmail()
                )
            )
        );

        verify(transaction, never())
            .complete(any(), any(), any());
    }

    @Test
    void handle_AlreadyUploaded_IsIdempotent() {
        User owner = MediaTestFixtures.owner();
        MediaAsset media =
            MediaTestFixtures.pendingMedia(owner);

        media.markUploaded(
            MediaTestFixtures.validMetadata()
        );

        prepareOwnerAndMedia(owner, media);

        MediaCompletionResponse response = handler.handle(
            new CompleteMediaUploadCommand(
                media.getPublicId(),
                owner.getEmail()
            )
        );

        assertEquals(
            MediaStatus.UPLOADED,
            response.status()
        );

        verify(objectStorage, never())
            .getMetadata(anyString());

        verify(validator, never())
            .validate(any(), any());

        verify(transaction, never())
            .complete(any(), any(), any());
    }

    @Test
    void handle_AttachedMedia_IsIdempotent() {
        User owner = MediaTestFixtures.owner();
        MediaAsset media =
            MediaTestFixtures.pendingMedia(owner);

        media.markUploaded(
            MediaTestFixtures.validMetadata()
        );
        media.markAttached();

        prepareOwnerAndMedia(owner, media);

        MediaCompletionResponse response = handler.handle(
            new CompleteMediaUploadCommand(
                media.getPublicId(),
                owner.getEmail()
            )
        );

        assertEquals(
            MediaStatus.ATTACHED,
            response.status()
        );

        verify(objectStorage, never())
            .getMetadata(anyString());

        verify(transaction, never())
            .complete(any(), any(), any());
    }

    @Test
    void handle_DeletedMedia_RejectsCompletion() {
        User owner = MediaTestFixtures.owner();
        MediaAsset media =
            MediaTestFixtures.pendingMedia(owner);

        media.markDeleted();

        prepareOwnerAndMedia(owner, media);

        assertThrows(
            InvalidMediaStateException.class,
            () -> handler.handle(
                new CompleteMediaUploadCommand(
                    media.getPublicId(),
                    owner.getEmail()
                )
            )
        );

        verify(objectStorage, never())
            .getMetadata(anyString());
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
                new CompleteMediaUploadCommand(
                    media.getPublicId(),
                    otherUser.getEmail()
                )
            )
        );

        verify(objectStorage, never())
            .getMetadata(anyString());

        verify(transaction, never())
            .complete(any(), any(), any());
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
