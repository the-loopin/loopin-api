package com.loopin.api.controller;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.common.security.JwtUtils;
import com.loopin.api.dto.auth.request.GoogleLoginRequest;
import com.loopin.api.entity.User;
import com.loopin.api.entity.UserProfile;
import com.loopin.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private ObjectMapper objectMapper;

    private User existingUser;
    private User existingAdmin;

    @BeforeEach
    void setUp() {
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
        GoogleLoginRequest request = new GoogleLoginRequest("google-id-456", "new@email.com", "New User");

        mockMvc.perform(post("/api/auth/google")
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
        GoogleLoginRequest request = new GoogleLoginRequest("google-id-123", "user@email.com", "John Doe");

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.email", is("user@email.com")))
                .andExpect(jsonPath("$.name", is("John Doe")))
                .andExpect(jsonPath("$.role", is("USER")));
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
}
