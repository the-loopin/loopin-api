package com.loopin.api.auth.dev;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dev/auth")
@Profile({"local", "dev", "test"})
@RequiredArgsConstructor
public class DevAuthController {

    private final DevLoginCommandHandler devLoginCommandHandler;

    @PostMapping("/login")
    public ResponseEntity<DevLoginResult> devLogin(
        @Valid @RequestBody DevLoginRequest request
    ) {
        DevLoginCommand command = new DevLoginCommand(request.email());

        DevLoginResult result = devLoginCommandHandler.handle(command);

        return ResponseEntity.ok(result);
    }

    public record DevLoginRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email

    ) {
    }
}
