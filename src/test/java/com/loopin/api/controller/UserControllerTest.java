package com.loopin.api.controller;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.dto.user.request.UpdateUserRoleRequest;
import com.loopin.api.dto.user.request.UserRegisterRequest;
import com.loopin.api.entity.User;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void registerUser_Success() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest("test@email.com", "Test User", "google-123");

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("test@email.com")))
                .andExpect(jsonPath("$.name", is("Test User")))
                .andExpect(jsonPath("$.googleId", is("google-123")))
                .andExpect(jsonPath("$.role", is("USER")));

        assertTrue(userRepository.findByEmail("test@email.com").isPresent());
    }

    @Test
    void registerUser_DuplicateEmail_ThrowsConflict() throws Exception {
        // Register first user
        UserRegisterRequest request1 = new UserRegisterRequest("duplicate@email.com", "User 1", null);
        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Register second user with same email
        UserRegisterRequest request2 = new UserRegisterRequest("duplicate@email.com", "User 2", null);
        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict());
    }

    @Test
    void getAllUsers_Admin_Success() throws Exception {
        // Setup users
        userRepository.save(new User("user1@email.com", "User 1", null));
        userRepository.save(new User("user2@email.com", "User 2", null));

        mockMvc.perform(get("/users")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getAllUsers_NonAdmin_ThrowsForbidden() throws Exception {
        mockMvc.perform(get("/users")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserById_Admin_Success() throws Exception {
        User user = userRepository.save(new User("user@email.com", "User", null));

        mockMvc.perform(get("/users/" + user.getId())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("user@email.com")));
    }

    @Test
    void getMyProfile_Success() throws Exception {
        User user = userRepository.save(new User("me@email.com", "Me", null));

        mockMvc.perform(get("/users/me")
                        .header("X-User-Id", user.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("me@email.com")));
    }

    @Test
    void getMyProfile_MissingHeader_ThrowsUnauthorized() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateUserRole_Admin_Success() throws Exception {
        User user = userRepository.save(new User("user@email.com", "User", null));
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(Role.ADMIN);

        mockMvc.perform(put("/users/" + user.getId() + "/role")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("ADMIN")));
    }

    @Test
    void deleteUser_Admin_SoftDeletesUser() throws Exception {
        User user = userRepository.save(new User("delete@email.com", "Delete Me", null));

        mockMvc.perform(delete("/users/" + user.getId())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isNoContent());

        // Active lookup should not find it
        assertFalse(userRepository.findByIdAndDeletedAtIsNull(user.getId()).isPresent());

        // Database still contains it, but marked as deleted
        var softDeletedUserOpt = userRepository.findById(user.getId());
        assertTrue(softDeletedUserOpt.isPresent());
        assertTrue(softDeletedUserOpt.get().isDeleted());
    }
}
