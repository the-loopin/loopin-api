package com.loopin.api.auth.dev;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record DevLoginCommand(

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email

) {
}
