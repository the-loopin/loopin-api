package com.loopin.api.dto.message.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateGroupMessageRequest {

    @NotBlank(message = "Message text is required")
    @Size(max = 1000, message = "Message text must not exceed 1000 characters")
    private String messageText;
}