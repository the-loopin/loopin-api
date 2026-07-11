package com.loopin.api.media.deletemedia;

import java.util.UUID;

public record DeleteMediaCommand(
    UUID mediaId,
    String currentUserEmail
) {
}
