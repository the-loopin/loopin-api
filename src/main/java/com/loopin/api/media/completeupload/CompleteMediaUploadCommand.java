package com.loopin.api.media.completeupload;

import java.util.UUID;

public record CompleteMediaUploadCommand(
    UUID mediaId,
    String currentUserEmail
) {
}
