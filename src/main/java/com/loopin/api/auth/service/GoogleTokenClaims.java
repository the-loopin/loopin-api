package com.loopin.api.auth.service;

public record GoogleTokenClaims(
        String googleId,
        String email,
        String name
) {
}
