package com.loopin.api.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EmbeddingRequest(
        String text,
        @JsonProperty("input_type") String inputType
) {
}
