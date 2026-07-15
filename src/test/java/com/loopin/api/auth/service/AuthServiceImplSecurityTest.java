package com.loopin.api.auth.service;

import com.loopin.api.auth.dto.AuthResponse;
import com.loopin.api.auth.dto.GoogleLoginRequest;
import com.loopin.api.auth.enums.Role;
import com.loopin.api.common.security.JwtUtils;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplSecurityTest {

    private static final String GOOGLE_ID = "google-user-123";
    private static final String EMAIL = "user@example.com";
    private static final String NAME = "Test User";
    private static final String ID_TOKEN = "verified-google-token";

    private UserRepository userRepository;
    private JwtUtils jwtUtils;
    private GoogleTokenVerifier googleTokenVerifier;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        jwtUtils = mock(JwtUtils.class);
        googleTokenVerifier = mock(GoogleTokenVerifier.class);

        authService = new AuthServiceImpl(
                userRepository,
                jwtUtils,
                googleTokenVerifier
        );

        when(googleTokenVerifier.verify(ID_TOKEN))
                .thenReturn(
                        new GoogleTokenClaims(
                                GOOGLE_ID,
                                EMAIL,
                                NAME
                        )
                );
    }

    @Test
    void deletedGoogleUserCannotLogin() {
        User user = activeUser();
        user.markAsDeleted();

        when(userRepository.findByGoogleId(GOOGLE_ID))
                .thenReturn(Optional.of(user));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> authService.authenticateGoogleUser(
                                request()
                        )
                );

        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode()
        );

        verify(jwtUtils, never())
                .generateToken(any(), any());

        assertFalse(
                user.getDeletedAt() == null,
                "Authentication must not restore a deleted account"
        );
    }

    @Test
    void inactiveGoogleUserCannotLogin() {
        User user = activeUser();
        user.setIsActive(false);

        when(userRepository.findByGoogleId(GOOGLE_ID))
                .thenReturn(Optional.of(user));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> authService.authenticateGoogleUser(
                                request()
                        )
                );

        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode()
        );

        verify(jwtUtils, never())
                .generateToken(any(), any());
    }

    @Test
    void deletedEmailLinkedUserCannotLoginOrBeLinked() {
        User user = new User(
                EMAIL,
                NAME,
                null
        );

        user.setRole(Role.USER);
        user.setIsActive(false);
        user.markAsDeleted();

        when(userRepository.findByGoogleId(GOOGLE_ID))
                .thenReturn(Optional.empty());

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        assertThrows(
                ResponseStatusException.class,
                () -> authService.authenticateGoogleUser(
                        request()
                )
        );

        verify(userRepository, never())
                .save(any(User.class));

        verify(jwtUtils, never())
                .generateToken(any(), any());
    }

    @Test
    void activeExistingUserCanLogin() {
        User user = activeUser();

        when(userRepository.findByGoogleId(GOOGLE_ID))
                .thenReturn(Optional.of(user));

        when(jwtUtils.generateToken(
                EMAIL,
                Role.USER.name()
        )).thenReturn("loopin-jwt");

        AuthResponse response =
                authService.authenticateGoogleUser(
                        request()
                );

        assertEquals(
                "loopin-jwt",
                response.getToken()
        );

        assertEquals(
                EMAIL,
                response.getEmail()
        );
    }

    @Test
    void verifiedGoogleLoginCreatesActiveUser() {
        when(userRepository.findByGoogleId(GOOGLE_ID))
                .thenReturn(Optional.empty());

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty());

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        when(jwtUtils.generateToken(
                EMAIL,
                Role.USER.name()
        )).thenReturn("loopin-jwt");

        AuthResponse response =
                authService.authenticateGoogleUser(
                        request()
                );

        assertEquals(
                "loopin-jwt",
                response.getToken()
        );

        verify(userRepository).save(
                any(User.class)
        );
    }

    private GoogleLoginRequest request() {
        return new GoogleLoginRequest(ID_TOKEN);
    }

    private User activeUser() {
        User user = new User(
                EMAIL,
                NAME,
                GOOGLE_ID
        );

        user.setRole(Role.USER);
        user.setIsActive(true);
        return user;
    }
}
