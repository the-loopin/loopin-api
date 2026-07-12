package com.loopin.api.integration;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.support.AbstractIntegrationTest;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DevAuthIntegrationTest extends AbstractIntegrationTest {

    private static final String CANONICAL_LOGIN_PATH = "/api/v1/dev/auth/login";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void testProfileExistingUserCanLogInWithoutJwtUsingCanonicalRoute() throws Exception {
        saveActiveUser("existing@example.com");

        mockMvc.perform(post(CANONICAL_LOGIN_PATH)
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"  EXISTING@EXAMPLE.COM  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.email", is("existing@example.com")));
    }

    @Test
    void unknownEmailReturnsNotFound() throws Exception {
        mockMvc.perform(post(CANONICAL_LOGIN_PATH)
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"missing@example.com\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void inactiveUserReturnsNotFoundWithoutIssuingAToken() throws Exception {
        User user = new User("inactive@example.com", "Inactive User", "google-inactive");
        user.setRole(Role.USER);
        user.setIsActive(false);
        userRepository.save(user);

        mockMvc.perform(post(CANONICAL_LOGIN_PATH)
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"inactive@example.com\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void blankOrInvalidEmailReturnsBadRequest() throws Exception {
        mockMvc.perform(post(CANONICAL_LOGIN_PATH)
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\" \"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(CANONICAL_LOGIN_PATH)
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void canonicalRouteIsMappedAndDuplicatedApiRouteIsNotMapped() throws Exception {
        MockHttpServletRequest canonical = requestFor("/api/v1/dev/auth/login");
        MockHttpServletRequest duplicate = requestFor("/api/api/v1/dev/auth/login");

        assertNotNull(handlerMapping.getHandler(canonical));
        assertNull(handlerMapping.getHandler(duplicate));
    }

    private User saveActiveUser(String email) {
        User user = new User(email, "Existing User", "google-" + email);
        user.setRole(Role.USER);
        user.setIsActive(true);
        return userRepository.save(user);
    }

    private MockHttpServletRequest requestFor(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setContextPath("/api");
        request.setServletPath(uri.substring("/api".length()));
        return request;
    }
}
