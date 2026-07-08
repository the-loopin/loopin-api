package com.loopin.api.support;

import com.loopin.api.auth.service.GoogleTokenVerifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.Mockito.mock;

@TestConfiguration(proxyBeanMethods = false)
public class IntegrationTestConfig {

    @Bean
    @Primary
    public GoogleTokenVerifier googleTokenVerifier() {
        return mock(GoogleTokenVerifier.class);
    }

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate() {
        return mock(StringRedisTemplate.class);
    }
}
