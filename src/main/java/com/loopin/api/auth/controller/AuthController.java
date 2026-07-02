package com.loopin.api.auth.controller;

import com.loopin.api.auth.dto.GoogleLoginRequest;
import com.loopin.api.auth.dto.AuthResponse;
import com.loopin.api.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> authenticateGoogleUser(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse response = authService.authenticateGoogleUser(request);
        return ResponseEntity.ok(response);
    }
}
