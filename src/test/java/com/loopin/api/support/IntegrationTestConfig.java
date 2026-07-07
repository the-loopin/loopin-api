package com.loopin.api.support;

import com.loopin.api.auth.service.GoogleTokenVerifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

@TestConfiguration(proxyBeanMethods = false)
public class IntegrationTestConfig {

    @Bean
    @Primary
    public GoogleTokenVerifier googleTokenVerifier() {
        return mock(GoogleTokenVerifier.class);
    }
}
