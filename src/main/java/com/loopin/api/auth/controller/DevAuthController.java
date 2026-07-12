package com.loopin.api.auth.controller;

import com.loopin.api.auth.dto.AuthResponse;
import com.loopin.api.common.security.JwtUtils;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@Profile("local")
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class DevAuthController {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public record DevLoginRequest(String email) {
    }

    @PostMapping("/dev-login")
    public ResponseEntity<AuthResponse> devLogin(
        @RequestBody DevLoginRequest request
    ) {
        User user = userRepository
            .findByEmailAndDeletedAtIsNull(request.email())
            .orElseThrow(() ->
                new NoSuchElementException(
                    "Seed user not found: " + request.email()
                )
            );

        String token = jwtUtils.generateToken(
            user.getEmail(),
            user.getRole().name()
        );

        return ResponseEntity.ok(
            new AuthResponse(
                token,
                user.getEmail(),
                user.getName(),
                user.getRole()
            )
        );
    }
}
