package com.loopin.api.auth.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Configuration
public class GoogleAuthConfig {

    @Bean
    GoogleIdTokenVerifier googleIdTokenVerifier(
        @Value("${google.client-id}") String googleClientId
    ) throws GeneralSecurityException, IOException {

        if (googleClientId == null || googleClientId.isBlank()) {
            throw new IllegalStateException(
                "Google OAuth client ID must be configured"
            );
        }

        return new GoogleIdTokenVerifier.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance()
        )
            .setAudience(List.of(googleClientId))
            .build();
    }
}
