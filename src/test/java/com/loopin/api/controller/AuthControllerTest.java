package com.loopin.api.controller;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.auth.service.GoogleTokenClaims;
import com.loopin.api.auth.service.GoogleTokenVerifier;
import com.loopin.api.common.security.JwtUtils;
import com.loopin.api.auth.dto.GoogleLoginRequest;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.entity.UserProfile;
import com.loopin.api.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private GoogleTokenVerifier googleTokenVerifier;

    @Autowired
    private ObjectMapper objectMapper;

    private User existingUser;
    private User existingAdmin;

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        GoogleTokenVerifier googleTokenVerifier() {
            return mock(GoogleTokenVerifier.class);
        }
    }

    @BeforeEach
    void setUp() {
        reset(googleTokenVerifier);
        when(googleTokenVerifier.verify("new-token"))
                .thenReturn(new GoogleTokenClaims("google-id-456", "new@email.com", "New User"));
        when(googleTokenVerifier.verify("existing-token"))
                .thenReturn(new GoogleTokenClaims("google-id-123", "user@email.com", "John Doe"));
        when(googleTokenVerifier.verify("link-token"))
                .thenReturn(new GoogleTokenClaims("new-google-id-789", "no-google-id@email.com", "No Google ID User"));
        when(googleTokenVerifier.verify("invalid-google-token"))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid Google ID token"));

        userRepository.deleteAll();

        // Setup a standard user
        existingUser = new User("user@email.com", "John Doe", "google-id-123");
        existingUser.setRole(Role.USER);
        UserProfile userProfile = new UserProfile();
        userProfile.setUser(existingUser);
        userProfile.setName("John Doe");
        existingUser.setProfile(userProfile);
        existingUser = userRepository.save(existingUser);

        // Setup an admin user
        existingAdmin = new User("admin@email.com", "Admin User", "google-id-999");
        existingAdmin.setRole(Role.ADMIN);
        UserProfile adminProfile = new UserProfile();
        adminProfile.setUser(existingAdmin);
        adminProfile.setName("Admin User");
        existingAdmin.setProfile(adminProfile);
        existingAdmin = userRepository.save(existingAdmin);
    }

    @Test
    void authenticateGoogleUser_NewUser_Success() throws Exception {
        GoogleLoginRequest request = new GoogleLoginRequest("new-token");

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.email", is("new@email.com")))
                .andExpect(jsonPath("$.name", is("New User")))
                .andExpect(jsonPath("$.role", is("USER")));

        assertTrue(userRepository.findByEmail("new@email.com").isPresent());
    }

    @Test
    void authenticateGoogleUser_ExistingUser_Success() throws Exception {
        GoogleLoginRequest request = new GoogleLoginRequest("existing-token");

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.email", is("user@email.com")))
                .andExpect(jsonPath("$.name", is("John Doe")))
                .andExpect(jsonPath("$.role", is("USER")));
    }

    @Test
    void authenticateGoogleUser_SoftDeletedUser_RestoresUserAndProfile() throws Exception {
        // Soft delete the user and profile
        existingUser.markAsDeleted();
        if (existingUser.getProfile() != null) {
            existingUser.getProfile().markAsDeleted();
        }
        existingUser = userRepository.save(existingUser);
        assertTrue(existingUser.isDeleted());
        assertTrue(existingUser.getProfile().isDeleted());

        GoogleLoginRequest request = new GoogleLoginRequest("existing-token");

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.email", is("user@email.com")))
                .andExpect(jsonPath("$.name", is("John Doe")))
                .andExpect(jsonPath("$.role", is("USER")));

        User restoredUser = userRepository.findByEmail("user@email.com").orElseThrow();
        org.junit.jupiter.api.Assertions.assertFalse(restoredUser.isDeleted());
        org.junit.jupiter.api.Assertions.assertFalse(restoredUser.getProfile().isDeleted());
    }

    @Test
    void authenticateGoogleUser_ExistingUserNoGoogleId_LinksGoogleId() throws Exception {
        // Setup user with no googleId
        User userWithoutGoogleId = new User("no-google-id@email.com", "No Google ID User", null);
        userWithoutGoogleId.setRole(Role.USER);
        UserProfile profile = new UserProfile();
        profile.setUser(userWithoutGoogleId);
        profile.setName("No Google ID User");
        userWithoutGoogleId.setProfile(profile);
        userWithoutGoogleId = userRepository.save(userWithoutGoogleId);

        GoogleLoginRequest request = new GoogleLoginRequest("link-token");

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.email", is("no-google-id@email.com")));

        User updatedUser = userRepository.findByEmail("no-google-id@email.com").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("new-google-id-789", updatedUser.getGoogleId());
    }

    @Test
    void authenticateGoogleUser_InvalidRequest_BadRequest() throws Exception {
        GoogleLoginRequest request = new GoogleLoginRequest("");

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void authenticateGoogleUser_InvalidGoogleToken_Unauthorized() throws Exception {
        GoogleLoginRequest request = new GoogleLoginRequest("invalid-google-token");

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessMeProfile_WithValidToken_Success() throws Exception {
        String token = jwtUtils.generateToken(existingUser.getEmail(), existingUser.getRole().name());

        mockMvc.perform(get("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("user@email.com")))
                .andExpect(jsonPath("$.name", is("John Doe")));
    }

    @Test
    void accessMeProfile_WithoutToken_ThrowsUnauthorized() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessUsersList_Admin_Success() throws Exception {
        String adminToken = jwtUtils.generateToken(existingAdmin.getEmail(), existingAdmin.getRole().name());

        mockMvc.perform(get("/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void accessUsersList_NormalUser_ThrowsForbidden() throws Exception {
        String userToken = jwtUtils.generateToken(existingUser.getEmail(), existingUser.getRole().name());

        mockMvc.perform(get("/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticateGoogleUser_WithInvalidTokenHeader_Success() throws Exception {
        GoogleLoginRequest request = new GoogleLoginRequest("existing-token");

        mockMvc.perform(post("/auth/google")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-or-expired-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()));
    }
}
