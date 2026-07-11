package com.loopin.api.media.shared.key;

import com.loopin.api.media.enums.MediaPurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaObjectKeyGeneratorTest {

    private MediaObjectKeyGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new MediaObjectKeyGenerator();
    }

    @Test
    void generate_UsesOwnerPurposeAndMediaIdentifier() {
        UUID ownerId = UUID.fromString(
            "11111111-1111-1111-1111-111111111111"
        );

        UUID mediaId = UUID.fromString(
            "22222222-2222-2222-2222-222222222222"
        );

        String result = generator.generate(
            ownerId,
            MediaPurpose.EVENT_IMAGE,
            mediaId
        );

        assertEquals(
            "media/"
                + ownerId
                + "/event_image/"
                + mediaId,
            result
        );
    }

    @Test
    void generate_DifferentMediaIdentifiers_ProduceDifferentKeys() {
        UUID ownerId = UUID.randomUUID();

        String first = generator.generate(
            ownerId,
            MediaPurpose.EVENT_IMAGE,
            UUID.randomUUID()
        );

        String second = generator.generate(
            ownerId,
            MediaPurpose.EVENT_IMAGE,
            UUID.randomUUID()
        );

        assertNotEquals(first, second);
    }

    @Test
    void generate_DoesNotContainOriginalFilename() {
        String result = generator.generate(
            UUID.randomUUID(),
            MediaPurpose.EVENT_IMAGE,
            UUID.randomUUID()
        );

        assertFalse(result.contains("event.webp"));
        assertFalse(result.contains(".jpg"));
        assertFalse(result.contains(".png"));
    }

    @Test
    void generate_ProfileAvatar_ContainsNormalizedPurpose() {
        String result = generator.generate(
            UUID.randomUUID(),
            MediaPurpose.PROFILE_AVATAR,
            UUID.randomUUID()
        );

        assertTrue(
            result.contains("/profile_avatar/")
        );
    }
}
