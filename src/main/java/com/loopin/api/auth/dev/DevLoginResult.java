package com.loopin.api.auth.dev;

import java.util.UUID;

public record DevLoginResult(
    String accessToken,
    String tokenType,
    UUID userId,
    String email
) {

    public static DevLoginResult of(
        String accessToken,
        UUID userId,
        String email
    ) {
        return new DevLoginResult(
            accessToken,
            "Bearer",
            userId,
            email
        );
    }
}
