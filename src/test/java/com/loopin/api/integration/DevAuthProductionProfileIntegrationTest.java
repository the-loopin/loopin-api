package com.loopin.api.integration;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.common.security.JwtUtils;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:dev_auth_production;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.liquibase.enabled=false",
    "jwt.secret=production-profile-test-secret-that-is-long-enough-for-hmac-signing",
    "google.client-id=production-profile-test-client",
    "cors.allowed-origins=*",
    "rate-limit.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("production")
class DevAuthProductionProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void productionProfileDoesNotRegisterDevLoginEndpoint() throws Exception {
        User user = new User("production-user@example.com", "Production User", "google-production-user");
        user.setRole(Role.USER);
        userRepository.save(user);
        String token = jwtUtils.generateToken(user.getEmail(), user.getRole().name());

        mockMvc.perform(post("/api/v1/dev/auth/login")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
