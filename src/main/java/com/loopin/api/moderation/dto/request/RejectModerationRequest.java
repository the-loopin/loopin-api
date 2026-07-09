package com.loopin.api.moderation.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectModerationRequest {

    @Size(max = 1000, message = "Rejection reason must not exceed 1000 characters")
    private String reason;
}
