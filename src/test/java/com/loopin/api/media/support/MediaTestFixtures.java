package com.loopin.api.media.support;

import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.enums.MediaPurpose;
import com.loopin.api.media.storage.StoredObjectMetadata;
import com.loopin.api.users.entity.User;

import java.util.UUID;

public final class MediaTestFixtures {

    public static final String OWNER_EMAIL =
        "owner@example.test";

    public static final String OTHER_EMAIL =
        "other@example.test";

    public static final UUID OWNER_PUBLIC_ID =
        UUID.fromString(
            "11111111-1111-1111-1111-111111111111"
        );

    public static final UUID OTHER_PUBLIC_ID =
        UUID.fromString(
            "22222222-2222-2222-2222-222222222222"
        );

    public static final UUID MEDIA_PUBLIC_ID =
        UUID.fromString(
            "33333333-3333-3333-3333-333333333333"
        );

    private MediaTestFixtures() {
    }

    public static User owner() {
        User user = new User(
            OWNER_EMAIL,
            "Owner",
            null
        );

        user.setId(1L);
        user.setPublicId(OWNER_PUBLIC_ID);

        return user;
    }

    public static User otherUser() {
        User user = new User(
            OTHER_EMAIL,
            "Other User",
            null
        );

        user.setId(2L);
        user.setPublicId(OTHER_PUBLIC_ID);

        return user;
    }

    public static MediaAsset pendingMedia() {
        return pendingMedia(owner());
    }

    public static MediaAsset pendingMedia(User owner) {
        return MediaAsset.pending(
            MEDIA_PUBLIC_ID,
            owner,
            "media/"
                + owner.getPublicId()
                + "/event_image/"
                + MEDIA_PUBLIC_ID,
            "event.webp",
            "image/webp",
            1_024L,
            MediaPurpose.EVENT_IMAGE
        );
    }

    public static StoredObjectMetadata validMetadata() {
        return new StoredObjectMetadata(
            1_024L,
            "image/webp",
            "\"test-etag\""
        );
    }
}
