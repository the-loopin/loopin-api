package com.loopin.api.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {

    private static final String INVALID_TOKEN_MESSAGE =
        "Invalid Google ID token";

    private static final String TEMPORARY_FAILURE_MESSAGE =
        "Google authentication is temporarily unavailable";

    private final GoogleIdTokenVerifier delegate;

    @Override
    public GoogleTokenClaims verify(String idToken) {
        validateTokenPresence(idToken);

        GoogleIdToken parsedToken = parseToken(idToken);
        verifyToken(parsedToken);

        GoogleIdToken.Payload payload = parsedToken.getPayload();

        validateVerifiedEmail(payload);

        String googleId = requireClaim(payload.getSubject());
        String email = requireClaim(payload.getEmail());
        String name = extractName(payload, email);

        return new GoogleTokenClaims(
            googleId,
            email,
            name
        );
    }

    private void validateTokenPresence(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw unauthorized();
        }
    }

    private GoogleIdToken parseToken(String idToken) {
        try {
            return GoogleIdToken.parse(
                GsonFactory.getDefaultInstance(),
                idToken
            );
        } catch (IOException | IllegalArgumentException exception) {
            throw unauthorized();
        }
    }

    private void verifyToken(GoogleIdToken parsedToken) {
        try {
            boolean valid = delegate.verify(parsedToken);

            if (!valid) {
                throw unauthorized();
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (IOException | GeneralSecurityException exception) {
            log.warn(
                "Google ID token verification temporarily failed",
                exception
            );

            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                TEMPORARY_FAILURE_MESSAGE
            );
        }
    }

    private void validateVerifiedEmail(
        GoogleIdToken.Payload payload
    ) {
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw unauthorized();
        }
    }

    private String requireClaim(String value) {
        if (value == null || value.isBlank()) {
            throw unauthorized();
        }

        return value;
    }

    private String extractName(
        GoogleIdToken.Payload payload,
        String fallbackEmail
    ) {
        Object nameClaim = payload.get("name");

        if (nameClaim instanceof String name && !name.isBlank()) {
            return name;
        }

        return fallbackEmail;
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            INVALID_TOKEN_MESSAGE
        );
    }
}
