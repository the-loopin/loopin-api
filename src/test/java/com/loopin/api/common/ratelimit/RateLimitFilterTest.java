package com.loopin.api.common.ratelimit;

import com.loopin.api.auth.dto.GoogleLoginRequest;
import com.loopin.api.auth.enums.Role;
import com.loopin.api.entity.User;
import com.loopin.api.entity.UserProfile;
import com.loopin.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "rate-limit.enabled=true",
        "rate-limit.policies[0].name=auth",
        "rate-limit.policies[0].requests=1",
        "rate-limit.policies[0].window=1m",
        "rate-limit.policies[0].methods[0]=POST",
        "rate-limit.policies[0].paths[0]=/auth/**"
})
@AutoConfigureMockMvc
@Transactional
class RateLimitFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User user = new User("user@email.com", "John Doe", "google-id-123");
        user.setRole(Role.USER);

        UserProfile userProfile = new UserProfile();
        userProfile.setUser(user);
        userProfile.setName("John Doe");
        user.setProfile(userProfile);

        userRepository.save(user);
    }

    @Test
    void requestsWithinConfiguredLimitKeepExistingBehavior() throws Exception {
        GoogleLoginRequest request = new GoogleLoginRequest("google-id-123", "user@email.com", "John Doe");

        mockMvc.perform(post("/auth/google")
                        .with(servletRequest -> {
                            servletRequest.setRemoteAddr("192.0.2.10");
                            return servletRequest;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()));
    }

    @Test
    void requestsExceedingConfiguredLimitReturnTooManyRequests() throws Exception {
        GoogleLoginRequest request = new GoogleLoginRequest("google-id-123", "user@email.com", "John Doe");

        mockMvc.perform(post("/auth/google")
                        .with(servletRequest -> {
                            servletRequest.setRemoteAddr("192.0.2.20");
                            return servletRequest;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/google")
                        .with(servletRequest -> {
                            servletRequest.setRemoteAddr("192.0.2.20");
                            return servletRequest;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message", containsString("Rate limit exceeded")));
    }

    @Test
    void untrustedForwardedForHeaderDoesNotBypassLimit() throws Exception {
        GoogleLoginRequest request = new GoogleLoginRequest("google-id-123", "user@email.com", "John Doe");

        mockMvc.perform(post("/auth/google")
                        .with(servletRequest -> {
                            servletRequest.setRemoteAddr("192.0.2.30");
                            return servletRequest;
                        })
                        .header("X-Forwarded-For", "198.51.100.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/google")
                        .with(servletRequest -> {
                            servletRequest.setRemoteAddr("192.0.2.30");
                            return servletRequest;
                        })
                        .header("X-Forwarded-For", "198.51.100.11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message", containsString("Rate limit exceeded")));
    }
}
