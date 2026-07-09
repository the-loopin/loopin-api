package com.loopin.api.moderation.ai.dto;

import java.util.List;

/** Payload sent to an AI moderation provider. */
public record AiModerationRequest(List<String> textFields) {

    public AiModerationRequest {
        textFields = textFields == null ? List.of() : List.copyOf(textFields);
    }
}
