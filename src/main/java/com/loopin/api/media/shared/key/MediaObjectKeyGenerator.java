package com.loopin.api.media.shared.key;

import com.loopin.api.media.enums.MediaPurpose;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

@Component
public class MediaObjectKeyGenerator {

    public String generate(
        UUID ownerPublicId,
        MediaPurpose purpose,
        UUID mediaPublicId
    ) {
        return "media/%s/%s/%s".formatted(
            ownerPublicId,
            purpose.name().toLowerCase(Locale.ROOT),
            mediaPublicId
        );
    }
}
