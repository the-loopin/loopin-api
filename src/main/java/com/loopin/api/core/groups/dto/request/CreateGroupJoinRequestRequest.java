package com.loopin.api.core.groups.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateGroupJoinRequestRequest {

    @Size(max = 500, message = "Message must not exceed 500 characters")
    private String message;
}
