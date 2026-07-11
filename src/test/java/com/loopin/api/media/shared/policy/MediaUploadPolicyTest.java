package com.loopin.api.media.shared.policy;

import com.loopin.api.media.enums.MediaPurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaUploadPolicyTest {

    private static final long TWO_MIB =
        2L * 1024 * 1024;

    private static final long FIVE_MIB =
        5L * 1024 * 1024;

    private MediaUploadPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new MediaUploadPolicy();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "image/jpeg",
        "image/png",
        "image/webp"
    })
    void validateRequest_SupportedContentType_Accepts(
        String contentType
    ) {
        assertDoesNotThrow(() ->
            policy.validateRequest(
                MediaPurpose.EVENT_IMAGE,
                contentType,
                1_024L
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "image/gif",
        "image/svg+xml",
        "application/pdf",
        "application/octet-stream",
        "video/mp4",
        "text/plain"
    })
    void validateRequest_UnsupportedContentType_Rejects(
        String contentType
    ) {
        assertThrows(
            IllegalArgumentException.class,
            () -> policy.validateRequest(
                MediaPurpose.EVENT_IMAGE,
                contentType,
                1_024L
            )
        );
    }

    @Test
    void validateRequest_ProfileAvatarExactlyAtLimit_Accepts() {
        assertDoesNotThrow(() ->
            policy.validateRequest(
                MediaPurpose.PROFILE_AVATAR,
                "image/png",
                TWO_MIB
            )
        );
    }

    @Test
    void validateRequest_ProfileAvatarAboveLimit_Rejects() {
        assertThrows(
            IllegalArgumentException.class,
            () -> policy.validateRequest(
                MediaPurpose.PROFILE_AVATAR,
                "image/png",
                TWO_MIB + 1
            )
        );
    }

    @ParameterizedTest
    @EnumSource(
        value = MediaPurpose.class,
        names = {
            "EVENT_IMAGE",
            "GROUP_IMAGE"
        }
    )
    void validateRequest_EventAndGroupExactlyAtLimit_Accepts(
        MediaPurpose purpose
    ) {
        assertDoesNotThrow(() ->
            policy.validateRequest(
                purpose,
                "image/webp",
                FIVE_MIB
            )
        );
    }

    @ParameterizedTest
    @EnumSource(
        value = MediaPurpose.class,
        names = {
            "EVENT_IMAGE",
            "GROUP_IMAGE"
        }
    )
    void validateRequest_EventAndGroupAboveLimit_Rejects(
        MediaPurpose purpose
    ) {
        assertThrows(
            IllegalArgumentException.class,
            () -> policy.validateRequest(
                purpose,
                "image/webp",
                FIVE_MIB + 1
            )
        );
    }

    @Test
    void validateRequest_ZeroSize_Rejects() {
        assertThrows(
            IllegalArgumentException.class,
            () -> policy.validateRequest(
                MediaPurpose.EVENT_IMAGE,
                "image/webp",
                0L
            )
        );
    }

    @Test
    void validateRequest_NegativeSize_Rejects() {
        assertThrows(
            IllegalArgumentException.class,
            () -> policy.validateRequest(
                MediaPurpose.EVENT_IMAGE,
                "image/webp",
                -1L
            )
        );
    }

    @Test
    void maximumSizeFor_ProfileAvatar_ReturnsTwoMib() {
        assertEquals(
            TWO_MIB,
            policy.maximumSizeFor(
                MediaPurpose.PROFILE_AVATAR
            )
        );
    }

    @Test
    void maximumSizeFor_EventImage_ReturnsFiveMib() {
        assertEquals(
            FIVE_MIB,
            policy.maximumSizeFor(
                MediaPurpose.EVENT_IMAGE
            )
        );
    }

    @Test
    void maximumSizeFor_GroupImage_ReturnsFiveMib() {
        assertEquals(
            FIVE_MIB,
            policy.maximumSizeFor(
                MediaPurpose.GROUP_IMAGE
            )
        );
    }
}
