package com.loopin.api.controller;

import com.loopin.api.dto.auth.request.GoogleLoginRequest;
import com.loopin.api.dto.auth.response.AuthResponse;
import com.loopin.api.service.abstraction.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/auth"})
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> authenticateGoogleUser(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse response = authService.authenticateGoogleUser(request);
        return ResponseEntity.ok(response);
    }
}
