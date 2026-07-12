package com.loopin.api.integration;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.common.security.JwtUtils;
import com.loopin.api.support.AbstractIntegrationTest;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventRequestValidationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private String authorization;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User user = new User("event-validation@example.com", "Event Validator", "google-event-validation");
        user.setRole(Role.USER);
        userRepository.save(user);
        authorization = "Bearer " + jwtUtils.generateToken(user.getEmail(), user.getRole().name());
    }

    @Test
    void createReturnsStandardBadRequestForCrossFieldValidation() throws Exception {
        mockMvc.perform(post("/v1/events")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Validation event",
                                  "description": "Validation description",
                                  "type": "EVENT",
                                  "category": "TECH",
                                  "city": "Baku",
                                  "startDateTime": "2020-01-01T10:00:00",
                                  "endDateTime": "2020-01-01T12:00:00",
                                  "isFree": true,
                                  "price": 0,
                                  "organizerName": "Loopin",
                                  "imageUrl": "ftp://example.com/image.jpg"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.fieldErrors.startDateTime").exists())
                .andExpect(jsonPath("$.fieldErrors.imageUrl").exists());
    }

    @Test
    void updateReturnsStandardBadRequestBeforeEventLookup() throws Exception {
        mockMvc.perform(put("/v1/events/{id}", UUID.randomUUID())
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Validation event",
                                  "description": "Validation description",
                                  "type": "EVENT",
                                  "category": "TECH",
                                  "city": "Baku",
                                  "startDateTime": "2030-01-01T10:00:00",
                                  "endDateTime": "2030-01-01T12:00:00",
                                  "isFree": false,
                                  "price": 0,
                                  "organizerName": "Loopin"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.fieldErrors.price").exists());
    }
}
