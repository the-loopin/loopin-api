package com.loopin.api.common.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.actuate.endpoint.HealthEndpointGroups;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(ActuatorHealthEndpointTest.FailingDownstreamConfiguration.class)
class ActuatorHealthEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HealthEndpointGroups healthEndpointGroups;

    @Test
    void livenessIsPublicAndStaysUpWhenADownstreamDependencyFails() throws Exception {
        mockMvc.perform(get("/api/actuator/health/liveness").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void readinessIsPublicAndReportsApplicationReadiness() throws Exception {
        mockMvc.perform(get("/api/actuator/health/readiness").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void readinessIncludesTheDatabaseButLivenessDoesNot() {
        assertTrue(healthEndpointGroups.get("readiness").isMember("db"));
        assertFalse(healthEndpointGroups.get("liveness").isMember("db"));
    }

    @Test
    void stagingAndProductionReadinessGroupsRequireRedis() {
        assertEquals("readinessState,db,redis", readinessMembers("application-staging.yaml"));
        assertEquals("readinessState,db,redis", readinessMembers("application-production.yaml"));
    }

    @Test
    void aggregateHealthAndSensitiveActuatorEndpointsAreNotPublic() throws Exception {
        mockMvc.perform(get("/api/actuator/health").contextPath("/api"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/actuator/env").contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @TestConfiguration
    static class FailingDownstreamConfiguration {

        @Bean
        HealthIndicator temporaryDownstreamHealthIndicator() {
            return () -> Health.down().withDetail("reason", "temporary failure").build();
        }
    }

    private String readinessMembers(String resourceName) {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource(resourceName));
        Properties properties = yaml.getObject();
        return properties.getProperty("management.endpoint.health.group.readiness.include");
    }
}
