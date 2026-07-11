package com.loopin.api.auth.controller;

import com.loopin.api.auth.dto.AuthResponse;
import com.loopin.api.auth.enums.Role;
import com.loopin.api.common.security.JwtUtils;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.entity.UserProfile;
import com.loopin.api.users.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("local")
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class DevAuthController {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    @PostMapping("/dev-login")
    public ResponseEntity<AuthResponse> devLogin(@Valid @RequestBody DevLoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .map(existingUser -> updateExistingUser(existingUser, request))
                .orElseGet(() -> createUser(request));

        String token = jwtUtils.generateToken(user.getEmail(), user.getRole().name());
        return ResponseEntity.ok(new AuthResponse(token, user.getEmail(), user.getName(), user.getRole()));
    }

    private User updateExistingUser(User user, DevLoginRequest request) {
        if (user.isDeleted()) {
            user.restore();
            if (user.getProfile() != null) {
                user.getProfile().restore();
            }
        }

        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name());
            if (user.getProfile() != null) {
                user.getProfile().setName(request.name());
            }
        }

        user.setRole(request.role() == null ? Role.USER : request.role());
        return userRepository.save(user);
    }

    private User createUser(DevLoginRequest request) {
        User user = new User(request.email(), request.name(), null);
        user.setRole(request.role() == null ? Role.USER : request.role());

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setName(request.name());
        user.setProfile(profile);

        return userRepository.save(user);
    }

    public record DevLoginRequest(
            @Email @NotBlank String email,
            @NotBlank String name,
            Role role
    ) {
    }
}
