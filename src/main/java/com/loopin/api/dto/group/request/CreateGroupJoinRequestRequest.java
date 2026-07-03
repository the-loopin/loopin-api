package com.loopin.api.dto.group.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateGroupJoinRequestRequest {

    @Size(max = 500, message = "Message must not exceed 500 characters")
    private String message;
}
