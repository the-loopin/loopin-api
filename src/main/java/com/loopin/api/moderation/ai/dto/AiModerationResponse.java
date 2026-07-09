package com.loopin.api.moderation.ai.dto;

/** Expected response shape from an AI moderation provider. */
public record AiModerationResponse(Boolean risky, String reason) {
}
