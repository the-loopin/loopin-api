package com.loopin.api.common.security;

import com.loopin.api.auth.enums.Role;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SecurityContext {

    public void requireAdmin(String roleHeader) {
        if (roleHeader == null || !roleHeader.equalsIgnoreCase(Role.ADMIN.name())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. Admin role required.");
        }
    }

    public void requireAuthenticatedUser(Long userIdHeader) {
        if (userIdHeader == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized. User ID header missing.");
        }
    }

    public void requireSelfOrAdmin(Long targetUserId, Long userIdHeader, String roleHeader) {
        requireAuthenticatedUser(userIdHeader);
        if (roleHeader != null && roleHeader.equalsIgnoreCase(Role.ADMIN.name())) {
            return;
        }
        if (!targetUserId.equals(userIdHeader)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. You cannot access this user's data.");
        }
    }
}
