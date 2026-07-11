package com.loopin.api.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleTokenVerifierImplTest {

    @Mock
    private GoogleIdTokenVerifier delegate;

    private GoogleTokenVerifierImpl verifier;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        verifier = new GoogleTokenVerifierImpl(delegate);
    }

    @Test
    void shouldReturnClaimsForValidToken() throws Exception {
        String rawToken = createToken(Map.of(
            "sub", "google-user-123",
            "email", "user@example.com",
            "email_verified", true,
            "name", "Test User",
            "iss", "https://accounts.google.com",
            "aud", "loopin-client-id",
            "iat", Instant.now().minusSeconds(60).getEpochSecond(),
            "exp", Instant.now().plusSeconds(3600).getEpochSecond()
        ));

        when(delegate.verify(any(GoogleIdToken.class)))
            .thenReturn(true);

        GoogleTokenClaims claims = verifier.verify(rawToken);

        assertEquals("google-user-123", claims.googleId());
        assertEquals("user@example.com", claims.email());
        assertEquals("Test User", claims.name());
    }

    @Test
    void shouldUseEmailWhenNameIsMissing() throws Exception {
        Map<String, Object> payload = validPayload();
        payload.remove("name");

        String rawToken = createToken(payload);

        when(delegate.verify(any(GoogleIdToken.class)))
            .thenReturn(true);

        GoogleTokenClaims claims = verifier.verify(rawToken);

        assertEquals("user@example.com", claims.name());
    }

    @Test
    void shouldRejectMalformedToken() {
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> verifier.verify("not-a-valid-jwt")
        );

        assertEquals(
            HttpStatus.UNAUTHORIZED,
            exception.getStatusCode()
        );

        verifyNoInteractions(delegate);
    }

    @Test
    void shouldRejectTokenWhenOfficialVerifierRejectsIt()
        throws Exception {
        String rawToken = createToken(validPayload());

        when(delegate.verify(any(GoogleIdToken.class)))
            .thenReturn(false);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> verifier.verify(rawToken)
        );

        assertEquals(
            HttpStatus.UNAUTHORIZED,
            exception.getStatusCode()
        );
    }

    @Test
    void shouldRejectUnverifiedEmail() throws Exception {
        Map<String, Object> payload = validPayload();
        payload.put("email_verified", false);

        String rawToken = createToken(payload);

        when(delegate.verify(any(GoogleIdToken.class)))
            .thenReturn(true);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> verifier.verify(rawToken)
        );

        assertEquals(
            HttpStatus.UNAUTHORIZED,
            exception.getStatusCode()
        );
    }

    @Test
    void shouldRejectMissingSubject() throws Exception {
        Map<String, Object> payload = validPayload();
        payload.remove("sub");

        String rawToken = createToken(payload);

        when(delegate.verify(any(GoogleIdToken.class)))
            .thenReturn(true);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> verifier.verify(rawToken)
        );

        assertEquals(
            HttpStatus.UNAUTHORIZED,
            exception.getStatusCode()
        );
    }

    @Test
    void shouldRejectMissingEmail() throws Exception {
        Map<String, Object> payload = validPayload();
        payload.remove("email");

        String rawToken = createToken(payload);

        when(delegate.verify(any(GoogleIdToken.class)))
            .thenReturn(true);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> verifier.verify(rawToken)
        );

        assertEquals(
            HttpStatus.UNAUTHORIZED,
            exception.getStatusCode()
        );
    }

    @Test
    void shouldReturnServiceUnavailableWhenCertificatesCannotBeLoaded()
        throws Exception {
        String rawToken = createToken(validPayload());

        when(delegate.verify(any(GoogleIdToken.class)))
            .thenThrow(new IOException("Google endpoint unavailable"));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> verifier.verify(rawToken)
        );

        assertEquals(
            HttpStatus.SERVICE_UNAVAILABLE,
            exception.getStatusCode()
        );
    }

    @Test
    void shouldReturnServiceUnavailableForSecurityInfrastructureFailure()
        throws Exception {
        String rawToken = createToken(validPayload());

        when(delegate.verify(any(GoogleIdToken.class)))
            .thenThrow(new GeneralSecurityException(
                "Cryptographic provider failure"
            ));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> verifier.verify(rawToken)
        );

        assertEquals(
            HttpStatus.SERVICE_UNAVAILABLE,
            exception.getStatusCode()
        );
    }

    private Map<String, Object> validPayload() {
        Map<String, Object> payload = new HashMap<>();

        payload.put("sub", "google-user-123");
        payload.put("email", "user@example.com");
        payload.put("email_verified", true);
        payload.put("name", "Test User");
        payload.put("iss", "https://accounts.google.com");
        payload.put("aud", "loopin-client-id");
        payload.put(
            "iat",
            Instant.now().minusSeconds(60).getEpochSecond()
        );
        payload.put(
            "exp",
            Instant.now().plusSeconds(3600).getEpochSecond()
        );

        return payload;
    }

    private String createToken(Map<String, Object> payload)
        throws JsonProcessingException {
        Map<String, Object> header = Map.of(
            "alg", "RS256",
            "kid", "test-key-id"
        );

        String encodedHeader = encode(
            objectMapper.writeValueAsString(header)
        );

        String encodedPayload = encode(
            objectMapper.writeValueAsString(payload)
        );

        String encodedSignature = encode("test-signature");

        return encodedHeader
            + "."
            + encodedPayload
            + "."
            + encodedSignature;
    }

    private String encode(String value) {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                value.getBytes(StandardCharsets.UTF_8)
            );
    }
}
