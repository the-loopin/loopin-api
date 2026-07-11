package com.loopin.api.media.shared.policy;

import com.loopin.api.media.enums.MediaPurpose;
import org.springframework.stereotype.Component;

import java.util.Set;

import static com.loopin.api.media.enums.MediaPurpose.PROFILE_AVATAR;

@Component
public class MediaUploadPolicy {

    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
        "image/jpeg",
        "image/png",
        "image/webp"
    );

    private static final long FIVE_MIB = 5L * 1024 * 1024;
    private static final long TWO_MIB = 2L * 1024 * 1024;

    public void validateRequest(
        MediaPurpose purpose,
        String contentType,
        long sizeBytes
    ) {
        if (!SUPPORTED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                "Unsupported media content type"
            );
        }

        if (sizeBytes <= 0) {
            throw new IllegalArgumentException(
                "File size must be greater than zero"
            );
        }

        long maximumSize = maximumSizeFor(purpose);

        if (sizeBytes > maximumSize) {
            throw new IllegalArgumentException(
                "File exceeds the maximum size for " + purpose
            );
        }
    }

    public long maximumSizeFor(MediaPurpose purpose) {
        return switch (purpose) {
            case EVENT_IMAGE, GROUP_IMAGE -> FIVE_MIB;
            case PROFILE_AVATAR -> TWO_MIB;
        };
    }

    public boolean supports(String contentType) {
        return SUPPORTED_CONTENT_TYPES.contains(contentType);
    }
}
