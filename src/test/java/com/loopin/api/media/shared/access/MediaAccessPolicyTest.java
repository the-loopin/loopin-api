package com.loopin.api.media.shared.access;

import com.loopin.api.common.exception.ForbiddenAccessException;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.support.MediaTestFixtures;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaAccessPolicyTest {

    private MediaAccessPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new MediaAccessPolicy();
    }

    @Test
    void requireOwner_ActualOwner_AllowsAccess() {
        User owner = MediaTestFixtures.owner();

        MediaAsset media =
            MediaTestFixtures.pendingMedia(owner);

        assertDoesNotThrow(() ->
            policy.requireOwner(media, owner)
        );
    }

    @Test
    void requireOwner_DifferentUser_RejectsAccess() {
        User owner = MediaTestFixtures.owner();

        User otherUser =
            MediaTestFixtures.otherUser();

        MediaAsset media =
            MediaTestFixtures.pendingMedia(owner);

        assertThrows(
            ForbiddenAccessException.class,
            () -> policy.requireOwner(
                media,
                otherUser
            )
        );
    }
}
