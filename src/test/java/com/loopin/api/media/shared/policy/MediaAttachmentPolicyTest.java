package com.loopin.api.media.shared.attachment;

import com.loopin.api.common.exception.ForbiddenAccessException;
import com.loopin.api.common.exception.InvalidMediaStateException;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.enums.MediaPurpose;
import com.loopin.api.media.shared.access.MediaAccessPolicy;
import com.loopin.api.media.storage.StoredObjectMetadata;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.loopin.api.media.enums.MediaPurpose.EVENT_IMAGE;
import static com.loopin.api.media.enums.MediaPurpose.GROUP_IMAGE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaAttachmentPolicyTest {

    private MediaAttachmentPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new MediaAttachmentPolicy(
            new MediaAccessPolicy()
        );
    }

    @Test
    void requireNewAttachment_acceptsUploadedMediaOwnedByExpectedUser() {
        User owner = user(1L, "owner@loopin.test");

        MediaAsset media = uploadedMedia(
            owner,
            EVENT_IMAGE
        );

        assertDoesNotThrow(() ->
            policy.requireNewAttachment(
                media,
                owner,
                EVENT_IMAGE
            )
        );
    }

    @Test
    void requireNewAttachment_rejectsMediaOwnedByAnotherUser() {
        User owner = user(1L, "owner@loopin.test");
        User anotherUser = user(2L, "other@loopin.test");

        MediaAsset media = uploadedMedia(
            owner,
            EVENT_IMAGE
        );

        assertThrows(
            ForbiddenAccessException.class,
            () -> policy.requireNewAttachment(
                media,
                anotherUser,
                EVENT_IMAGE
            )
        );
    }

    @Test
    void requireNewAttachment_rejectsUnexpectedPurpose() {
        User owner = user(1L, "owner@loopin.test");

        MediaAsset media = uploadedMedia(
            owner,
            GROUP_IMAGE
        );

        assertThrows(
            InvalidMediaStateException.class,
            () -> policy.requireNewAttachment(
                media,
                owner,
                EVENT_IMAGE
            )
        );
    }

    @Test
    void requireNewAttachment_rejectsPendingMedia() {
        User owner = user(1L, "owner@loopin.test");

        MediaAsset media = pendingMedia(
            owner,
            EVENT_IMAGE
        );

        assertThrows(
            InvalidMediaStateException.class,
            () -> policy.requireNewAttachment(
                media,
                owner,
                EVENT_IMAGE
            )
        );
    }

    @Test
    void requireNewAttachment_rejectsAlreadyAttachedMedia() {
        User owner = user(1L, "owner@loopin.test");

        MediaAsset media = uploadedMedia(
            owner,
            EVENT_IMAGE
        );

        media.markAttached();

        assertThrows(
            InvalidMediaStateException.class,
            () -> policy.requireNewAttachment(
                media,
                owner,
                EVENT_IMAGE
            )
        );
    }

    @Test
    void requireExistingAttachment_acceptsAttachedMedia() {
        User owner = user(1L, "owner@loopin.test");

        MediaAsset media = uploadedMedia(
            owner,
            EVENT_IMAGE
        );

        media.markAttached();

        assertDoesNotThrow(() ->
            policy.requireExistingAttachment(
                media,
                EVENT_IMAGE
            )
        );
    }

    @Test
    void requireExistingAttachment_rejectsUploadedButUnattachedMedia() {
        User owner = user(1L, "owner@loopin.test");

        MediaAsset media = uploadedMedia(
            owner,
            EVENT_IMAGE
        );

        assertThrows(
            InvalidMediaStateException.class,
            () -> policy.requireExistingAttachment(
                media,
                EVENT_IMAGE
            )
        );
    }

    @Test
    void requireExistingAttachment_rejectsUnexpectedPurpose() {
        User owner = user(1L, "owner@loopin.test");

        MediaAsset media = uploadedMedia(
            owner,
            GROUP_IMAGE
        );

        media.markAttached();

        assertThrows(
            InvalidMediaStateException.class,
            () -> policy.requireExistingAttachment(
                media,
                EVENT_IMAGE
            )
        );
    }

    private MediaAsset uploadedMedia(
        User owner,
        MediaPurpose purpose
    ) {
        MediaAsset media = pendingMedia(
            owner,
            purpose
        );

        media.markUploaded(
            new StoredObjectMetadata(
                100_000L,
                "image/webp",
                "test-etag"
            )
        );

        return media;
    }

    private MediaAsset pendingMedia(
        User owner,
        MediaPurpose purpose
    ) {
        UUID mediaId = UUID.randomUUID();

        return MediaAsset.pending(
            mediaId,
            owner,
            "media/"
                + purpose.name().toLowerCase()
                + "/"
                + mediaId
                + ".webp",
            "image.webp",
            "image/webp",
            100_000L,
            purpose
        );
    }

    private User user(
        Long id,
        String email
    ) {
        User user = new User(
            email,
            "User",
            null
        );

        user.setId(id);

        return user;
    }
}
