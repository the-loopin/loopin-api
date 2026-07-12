package com.loopin.api.auth.dev;

import com.loopin.api.common.security.JwtUtils;
import com.loopin.api.users.entity.User;
import com.loopin.api.common.exception.UserNotFoundException;
import com.loopin.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DevLoginCommandHandler {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    @Transactional(readOnly = true)
    public DevLoginResult handle(DevLoginCommand command) {
        String normalizedEmail = command.email()
            .trim()
            .toLowerCase();

        User user = userRepository
            .findByEmailAndDeletedAtIsNull(normalizedEmail)
            .orElseThrow(() -> new UserNotFoundException(normalizedEmail));

        String token = jwtUtils.generateToken(
            user.getEmail(),
            user.getRole().name()
        );

        return new DevLoginResult(
            token,
            "Bearer",
            user.getPublicId(),
            user.getEmail()
        );
    }
}
