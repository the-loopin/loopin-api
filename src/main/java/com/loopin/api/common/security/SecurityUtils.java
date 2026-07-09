package com.loopin.api.common.security;

import com.loopin.api.core.users.entity.User;
import com.loopin.api.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
        // Private constructor to prevent instantiation of utility class
    }

    /**
     * Retrieve the email (username) of the currently authenticated user,
     * or throw UnauthorizedException if not authenticated.
     *
     * @return the email of the authenticated user
     * @throws UnauthorizedException if the user is not authenticated
     */
    public static String getRequiredCurrentUserEmail() {
        return getCurrentUserEmail()
                .orElseThrow(() -> new UnauthorizedException("Authentication is required"));
    }

    /**
     * Retrieve the email (username) of the currently authenticated user.
     *
     * @return an Optional containing the email of the authenticated user, or empty if not authenticated
     */
    public static Optional<String> getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return Optional.of(((UserDetails) principal).getUsername());
        } else if (principal instanceof String) {
            return Optional.of((String) principal);
        }

        return Optional.empty();
    }

    /**
     * Retrieve the currently authenticated User entity.
     *
     * @return an Optional containing the User entity, or empty if not authenticated
     */
    public static Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return Optional.of(((CustomUserDetails) principal).getUser());
        }

        return Optional.empty();
    }
}
