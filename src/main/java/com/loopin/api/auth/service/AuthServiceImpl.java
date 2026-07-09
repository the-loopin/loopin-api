package com.loopin.api.auth.service;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.common.security.JwtUtils;
import com.loopin.api.auth.dto.GoogleLoginRequest;
import com.loopin.api.auth.dto.AuthResponse;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.entity.UserProfile;
import com.loopin.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final GoogleTokenVerifier googleTokenVerifier;

    @Override
    @Transactional
    public AuthResponse authenticateGoogleUser(GoogleLoginRequest request) {
        GoogleTokenClaims claims = googleTokenVerifier.verify(request.getIdToken());
        User user;

        // 1. Try to find user by googleId
        Optional<User> userByGoogleId = userRepository.findByGoogleId(claims.googleId());

        if (userByGoogleId.isPresent()) {
            user = userByGoogleId.get();
            if (user.isDeleted()) {
                user.restore();
                if (user.getProfile() != null) {
                    user.getProfile().restore();
                }
                userRepository.save(user);
            }
        } else {
            // 2. Try to find user by email
            Optional<User> userByEmail = userRepository.findByEmail(claims.email());

            if (userByEmail.isPresent()) {
                user = userByEmail.get();
                if (user.isDeleted()) {
                    user.restore();
                    if (user.getProfile() != null) {
                        user.getProfile().restore();
                    }
                }
                // Link the googleId if it wasn't linked yet
                if (user.getGoogleId() == null || user.getGoogleId().isBlank()) {
                    user.setGoogleId(claims.googleId());
                }
                userRepository.save(user);
            } else {
                // 3. Register a new user
                user = new User(claims.email(), claims.name(), claims.googleId());
                user.setRole(Role.USER); // Default role

                UserProfile profile = new UserProfile();
                profile.setUser(user);
                profile.setName(claims.name());
                user.setProfile(profile);

                user = userRepository.save(user);
            }
        }

        String token = jwtUtils.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getEmail(), user.getName(), user.getRole());
    }
}
