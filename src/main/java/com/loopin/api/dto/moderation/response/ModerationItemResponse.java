package com.loopin.api.dto.moderation.response;

import com.loopin.api.common.enums.ContentModerationStatus;
import com.loopin.api.common.enums.ModerationContentType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ModerationItemResponse {

    private final UUID id;
    private final ModerationContentType contentType;
    private final String title;
    private final String description;
    private final ContentModerationStatus moderationStatus;
    private final LocalDateTime createdAt;
}
