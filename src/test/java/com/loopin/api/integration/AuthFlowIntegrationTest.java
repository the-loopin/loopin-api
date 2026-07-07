package com.loopin.api.integration;

import com.loopin.api.auth.dto.GoogleLoginRequest;
import com.loopin.api.auth.enums.Role;
import com.loopin.api.auth.service.GoogleTokenClaims;
import com.loopin.api.auth.service.GoogleTokenVerifier;
import com.loopin.api.entity.User;
import com.loopin.api.entity.UserProfile;
import com.loopin.api.repository.UserRepository;
import com.loopin.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GoogleTokenVerifier googleTokenVerifier;

    @BeforeEach
    void setUp() {
        reset(googleTokenVerifier);
        userRepository.deleteAll();
    }

    @Test
    void newUserLoginViaGoogleCreatesUserAndReturnsToken() throws Exception {
        when(googleTokenVerifier.verify("valid-new-token"))
                .thenReturn(new GoogleTokenClaims("new-google-id", "new@email.com", "New User"));

        GoogleLoginRequest request = new GoogleLoginRequest("valid-new-token");

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.email", is("new@email.com")))
                .andExpect(jsonPath("$.name", is("New User")));

        User savedUser = userRepository.findByEmail("new@email.com").orElse(null);
        assertNotNull(savedUser);
        assertEquals("new-google-id", savedUser.getGoogleId());
    }

    @Test
    void existingUserLoginViaGoogleDoesNotDuplicateUserAndReturnsToken() throws Exception {
        User existingUser = new User("existing@email.com", "Existing User", "existing-google-id");
        existingUser.setRole(Role.USER);
        UserProfile profile = new UserProfile();
        profile.setUser(existingUser);
        profile.setName("Existing User");
        existingUser.setProfile(profile);
        userRepository.save(existingUser);

        when(googleTokenVerifier.verify("valid-existing-token"))
                .thenReturn(new GoogleTokenClaims("existing-google-id", "existing@email.com", "Existing User"));

        GoogleLoginRequest request = new GoogleLoginRequest("valid-existing-token");

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.email", is("existing@email.com")));

        long userCount = userRepository.count();
        assertEquals(1, userCount);
    }

    @Test
    void invalidGoogleTokenReturnsUnauthorized() throws Exception {
        when(googleTokenVerifier.verify("invalid-token"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google ID token"));

        GoogleLoginRequest request = new GoogleLoginRequest("invalid-token");

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void malformedRequestBodyReturnsBadRequest() throws Exception {
        // Missing idToken
        String requestBody = "{}";

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void authenticatedRequestToProtectedEndpointSucceeds() throws Exception {
        when(googleTokenVerifier.verify("valid-new-token"))
                .thenReturn(new GoogleTokenClaims("auth-google-id", "auth@email.com", "Auth User"));

        GoogleLoginRequest request = new GoogleLoginRequest("valid-new-token");

        MvcResult loginResult = mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        String token = JsonPath.read(responseBody, "$.token");

        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("auth@email.com")))
                .andExpect(jsonPath("$.name", is("Auth User")));
    }

    @Test
    void expiredOrTamperedJwtOnProtectedEndpointReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer invalid.tampered.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }
}
