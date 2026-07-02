package com.loopin.api.common.security;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserEmail_Unauthenticated_ReturnsEmpty() {
        Optional<String> email = SecurityUtils.getCurrentUserEmail();
        assertFalse(email.isPresent());
    }

    @Test
    void getCurrentUserEmail_AuthenticatedStringPrincipal_ReturnsEmail() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "test@email.com",
                null,
                Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<String> email = SecurityUtils.getCurrentUserEmail();
        assertTrue(email.isPresent());
        assertEquals("test@email.com", email.get());
    }

    @Test
    void getCurrentUserEmail_AuthenticatedUserDetailsPrincipal_ReturnsEmail() {
        User user = new User("test@email.com", "Test User", "google-123");
        user.setRole(Role.USER);
        UserDetails userDetails = new CustomUserDetails(user);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
            );
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<String> email = SecurityUtils.getCurrentUserEmail();
        assertTrue(email.isPresent());
        assertEquals("test@email.com", email.get());
    }

    @Test
    void getCurrentUser_Unauthenticated_ReturnsEmpty() {
        Optional<User> currentUser = SecurityUtils.getCurrentUser();
        assertFalse(currentUser.isPresent());
    }

    @Test
    void getCurrentUser_AuthenticatedCustomUserDetails_ReturnsUserEntity() {
        User user = new User("test@email.com", "Test User", "google-123");
        user.setRole(Role.USER);
        UserDetails userDetails = new CustomUserDetails(user);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<User> currentUser = SecurityUtils.getCurrentUser();
        assertTrue(currentUser.isPresent());
        assertEquals("test@email.com", currentUser.get().getEmail());
        assertEquals("Test User", currentUser.get().getName());
        assertEquals("google-123", currentUser.get().getGoogleId());
    }
}
