package com.loopin.api.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record EmbeddingBatchRequest(List<String> texts, @JsonProperty("input_type") String inputType) { }
