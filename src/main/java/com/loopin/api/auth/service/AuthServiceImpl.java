package com.loopin.api.auth.service;

import com.loopin.api.auth.dto.AuthResponse;
import com.loopin.api.auth.dto.GoogleLoginRequest;
import com.loopin.api.auth.enums.Role;
import com.loopin.api.common.security.JwtUtils;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.entity.UserProfile;
import com.loopin.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String DISABLED_ACCOUNT_MESSAGE =
            "This account has been disabled";

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final GoogleTokenVerifier googleTokenVerifier;

    @Override
    @Transactional
    public AuthResponse authenticateGoogleUser(
            GoogleLoginRequest request
    ) {
        GoogleTokenClaims claims =
                googleTokenVerifier.verify(
                        request.getIdToken()
                );

        User user = resolveUser(claims);

        String token = jwtUtils.generateToken(
                user.getEmail(),
                user.getRole().name()
        );

        return new AuthResponse(
                token,
                user.getEmail(),
                user.getName(),
                user.getRole()
        );
    }

    private User resolveUser(
            GoogleTokenClaims claims
    ) {
        Optional<User> userByGoogleId =
                userRepository.findByGoogleId(
                        claims.googleId()
                );

        if (userByGoogleId.isPresent()) {
            User user = userByGoogleId.get();
            requireActiveAccount(user);
            return user;
        }

        Optional<User> userByEmail =
                userRepository.findByEmail(
                        claims.email()
                );

        if (userByEmail.isPresent()) {
            User user = userByEmail.get();
            requireActiveAccount(user);

            if (user.getGoogleId() == null
                    || user.getGoogleId().isBlank()) {
                user.setGoogleId(claims.googleId());
                return userRepository.save(user);
            }

            return user;
        }

        return createUser(claims);
    }

    private User createUser(
            GoogleTokenClaims claims
    ) {
        User user = new User(
                claims.email(),
                claims.name(),
                claims.googleId()
        );

        user.setRole(Role.USER);
        user.setIsActive(true);

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setName(claims.name());
        user.setProfile(profile);

        return userRepository.save(user);
    }

    private void requireActiveAccount(
            User user
    ) {
        if (user.isDeleted()
                || !Boolean.TRUE.equals(
                user.getIsActive()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    DISABLED_ACCOUNT_MESSAGE
            );
        }
    }
}
