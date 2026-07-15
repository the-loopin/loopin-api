package com.loopin.api.controller;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.common.security.JwtUtils;
import com.loopin.api.users.dto.request.UpdateUserRoleRequest;
import com.loopin.api.users.dto.request.UserRegisterRequest;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.repository.UserRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Autowired
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void registerUser_Success()
            throws Exception {
        UserRegisterRequest request =
                new UserRegisterRequest(
                        "test@email.com",
                        "Test User"
                );

        mockMvc.perform(
                        post("/v1/users/register")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper
                                                .writeValueAsString(
                                                        request
                                                )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath(
                                "$.email",
                                is("test@email.com")
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.name",
                                is("Test User")
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.role",
                                is("USER")
                        )
                );

        assertTrue(
                userRepository
                        .findByEmail("test@email.com")
                        .isPresent()
        );
    }

    @Test
    void registerUser_DuplicateEmail_ThrowsConflict()
            throws Exception {
        UserRegisterRequest firstRequest =
                new UserRegisterRequest(
                        "duplicate@email.com",
                        "User 1"
                );

        mockMvc.perform(
                        post("/v1/users/register")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper
                                                .writeValueAsString(
                                                        firstRequest
                                                )
                                )
                )
                .andExpect(status().isCreated());

        UserRegisterRequest secondRequest =
                new UserRegisterRequest(
                        "duplicate@email.com",
                        "User 2"
                );

        mockMvc.perform(
                        post("/v1/users/register")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper
                                                .writeValueAsString(
                                                        secondRequest
                                                )
                                )
                )
                .andExpect(status().isConflict());
    }

    @Test
    void getAllUsers_Admin_Success()
            throws Exception {
        userRepository.save(
                new User(
                        "user1@email.com",
                        "User 1",
                        null
                )
        );

        userRepository.save(
                new User(
                        "user2@email.com",
                        "User 2",
                        null
                )
        );

        User admin = new User(
                "admin@email.com",
                "Admin",
                null
        );

        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        String token = jwtUtils.generateToken(
                admin.getEmail(),
                Role.ADMIN.name()
        );

        mockMvc.perform(
                        get("/v1/users")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$",
                                hasSize(3)
                        )
                );
    }

    @Test
    void getAllUsers_NonAdmin_ThrowsForbidden()
            throws Exception {
        User user = new User(
                "user@email.com",
                "User",
                null
        );

        user.setRole(Role.USER);
        userRepository.save(user);

        String token = jwtUtils.generateToken(
                user.getEmail(),
                Role.USER.name()
        );

        mockMvc.perform(
                        get("/v1/users")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserById_Admin_Success()
            throws Exception {
        User admin = new User(
                "admin@email.com",
                "Admin",
                null
        );

        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        String token = jwtUtils.generateToken(
                admin.getEmail(),
                Role.ADMIN.name()
        );

        User user = userRepository.save(
                new User(
                        "user@email.com",
                        "User",
                        null
                )
        );

        mockMvc.perform(
                        get(
                                "/v1/users/"
                                        + user.getPublicId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.email",
                                is("user@email.com")
                        )
                );
    }

    @Test
    void getMyProfile_Success()
            throws Exception {
        User user = userRepository.save(
                new User(
                        "me@email.com",
                        "Me",
                        null
                )
        );

        user.setRole(Role.USER);
        userRepository.save(user);

        String token = jwtUtils.generateToken(
                user.getEmail(),
                Role.USER.name()
        );

        mockMvc.perform(
                        get("/v1/users/me")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.email",
                                is("me@email.com")
                        )
                );
    }

    @Test
    void getMyProfile_MissingHeader_ThrowsUnauthorized()
            throws Exception {
        mockMvc.perform(
                        get("/v1/users/me")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateUserRole_Admin_Success()
            throws Exception {
        User admin = new User(
                "admin@email.com",
                "Admin",
                null
        );

        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        String token = jwtUtils.generateToken(
                admin.getEmail(),
                Role.ADMIN.name()
        );

        User user = userRepository.save(
                new User(
                        "user@email.com",
                        "User",
                        null
                )
        );

        UpdateUserRoleRequest request =
                new UpdateUserRoleRequest(
                        Role.ADMIN
                );

        mockMvc.perform(
                        put(
                                "/v1/users/"
                                        + user.getPublicId()
                                        + "/role"
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper
                                                .writeValueAsString(
                                                        request
                                                )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.role",
                                is("ADMIN")
                        )
                );
    }

    @Test
    void deleteUser_Admin_SoftDeletesAndDeactivatesUser()
            throws Exception {
        User admin = new User(
                "admin@email.com",
                "Admin",
                null
        );

        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        String token = jwtUtils.generateToken(
                admin.getEmail(),
                Role.ADMIN.name()
        );

        User user = userRepository.save(
                new User(
                        "delete@email.com",
                        "Delete Me",
                        null
                )
        );

        mockMvc.perform(
                        delete(
                                "/v1/users/"
                                        + user.getPublicId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isNoContent());

        assertFalse(
                userRepository
                        .findByIdAndDeletedAtIsNull(
                                user.getId()
                        )
                        .isPresent()
        );

        User deletedUser =
                userRepository
                        .findById(user.getId())
                        .orElseThrow();

        assertTrue(deletedUser.isDeleted());

        assertFalse(
                Boolean.TRUE.equals(
                        deletedUser.getIsActive()
                )
        );
    }
}
