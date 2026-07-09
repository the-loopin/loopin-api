package com.loopin.api.integration;

import com.jayway.jsonpath.JsonPath;
import com.loopin.api.auth.dto.GoogleLoginRequest;
import com.loopin.api.auth.enums.Role;
import com.loopin.api.auth.service.GoogleTokenClaims;
import com.loopin.api.auth.service.GoogleTokenVerifier;
import com.loopin.api.core.events.enums.EventCategory;
import com.loopin.api.core.events.enums.EventStatus;
import com.loopin.api.core.events.enums.EventType;
import com.loopin.api.core.events.dto.request.EventCreateRequest;
import com.loopin.api.core.events.dto.request.EventUpdateRequest;
import com.loopin.api.reports.dto.request.UpdateReportStatusRequest;
import com.loopin.api.reports.enums.ReportStatus;
import com.loopin.api.core.users.dto.request.UpdateUserRoleRequest;
import com.loopin.api.core.users.entity.User;
import com.loopin.api.core.users.entity.UserProfile;
import com.loopin.api.core.users.repository.UserRepository;
import com.loopin.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoleBasedAccessIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GoogleTokenVerifier googleTokenVerifier;

    private String userToken;
    private String adminToken;
    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() throws Exception {
        reset(googleTokenVerifier);
        userRepository.deleteAll();

        // 1. Seed a regular USER
        testUser = new User("user@email.com", "Regular User", "user-google-id");
        testUser.setRole(Role.USER);
        UserProfile userProfile = new UserProfile();
        userProfile.setUser(testUser);
        userProfile.setName("Regular User");
        testUser.setProfile(userProfile);
        userRepository.save(testUser);

        // 2. Seed an ADMIN
        adminUser = new User("admin@email.com", "Admin User", "admin-google-id");
        adminUser.setRole(Role.ADMIN);
        UserProfile adminProfile = new UserProfile();
        adminProfile.setUser(adminUser);
        adminProfile.setName("Admin User");
        adminUser.setProfile(adminProfile);
        userRepository.save(adminUser);

        // Mock verification for both tokens
        when(googleTokenVerifier.verify("valid-user-token"))
                .thenReturn(new GoogleTokenClaims("user-google-id", "user@email.com", "Regular User"));
        when(googleTokenVerifier.verify("valid-admin-token"))
                .thenReturn(new GoogleTokenClaims("admin-google-id", "admin@email.com", "Admin User"));

        // Obtain JWT for regular user
        GoogleLoginRequest userRequest = new GoogleLoginRequest("valid-user-token");
        MvcResult userLoginResult = mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isOk())
                .andReturn();
        userToken = JsonPath.read(userLoginResult.getResponse().getContentAsString(), "$.token");

        // Obtain JWT for admin
        GoogleLoginRequest adminRequest = new GoogleLoginRequest("valid-admin-token");
        MvcResult adminLoginResult = mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRequest)))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = JsonPath.read(adminLoginResult.getResponse().getContentAsString(), "$.token");
    }

    @Test
    void regularUserCannotAccessAdminEndpoints_ReturnsForbidden() throws Exception {
        UUID dummyId = UUID.randomUUID();
        
        UpdateUserRoleRequest roleRequest = new UpdateUserRoleRequest();
        roleRequest.setRole(Role.USER);
        
        UpdateReportStatusRequest reportRequest = new UpdateReportStatusRequest();
        reportRequest.setStatus(ReportStatus.RESOLVED);

        // Dashboard stats
        mockMvc.perform(get("/admin/dashboard/stats").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // List users
        mockMvc.perform(get("/admin/users").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // Update user role
        mockMvc.perform(put("/admin/users/" + dummyId + "/role")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleRequest)))
                .andExpect(status().isForbidden());

        // Delete user
        mockMvc.perform(delete("/admin/users/" + dummyId).header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // List events
        mockMvc.perform(get("/admin/events").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // Delete event
        mockMvc.perform(delete("/admin/events/" + dummyId).header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // List reports
        mockMvc.perform(get("/admin/reports").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // Update report status
        mockMvc.perform(patch("/admin/reports/" + dummyId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAdminEndpoints_ReturnsSuccess() throws Exception {
        UUID dummyId = testUser.getPublicId(); // Use a real ID for operations that might fetch it
        
        UpdateUserRoleRequest roleRequest = new UpdateUserRoleRequest();
        roleRequest.setRole(Role.USER);

        // Dashboard stats
        mockMvc.perform(get("/admin/dashboard/stats").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // List users
        mockMvc.perform(get("/admin/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Update user role (dummy ID)
        // Depending on service logic, might return 404 if not found, but we want to ensure it's not 401/403.
        // We use testUser.getId() so it exists.
        mockMvc.perform(put("/admin/users/" + dummyId + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleRequest)))
                .andExpect(status().isOk());

        // List events
        mockMvc.perform(get("/admin/events").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // List reports
        mockMvc.perform(get("/admin/reports").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
                
        // Delete user (we do this last so we don't break other tests depending on testUser)
        mockMvc.perform(delete("/admin/users/" + dummyId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void ownerOrAdminCanModifyEvent_NonOwnerCannot_ReturnsExpectedStatus() throws Exception {
        // 1. User A (testUser) creates an event
        EventCreateRequest createRequest = new EventCreateRequest();
        createRequest.setTitle("Test Event");
        createRequest.setDescription("Test Description");
        createRequest.setType(EventType.EVENT);
        createRequest.setCategory(EventCategory.TECH);
        createRequest.setCity("Dubai");
        createRequest.setStartDateTime(LocalDateTime.now().plusDays(1));
        createRequest.setEndDateTime(LocalDateTime.now().plusDays(2));
        createRequest.setIsFree(true);
        createRequest.setPrice(BigDecimal.ZERO);
        createRequest.setOrganizerName("Test Organizer");
        createRequest.setStatus(EventStatus.DRAFT);

        MvcResult createResult = mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String eventId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        // Prepare Update Request
        EventUpdateRequest updateRequest = new EventUpdateRequest();
        updateRequest.setTitle("Updated Event");
        updateRequest.setDescription("Updated Description");
        updateRequest.setType(EventType.EVENT);
        updateRequest.setCategory(EventCategory.TECH);
        updateRequest.setCity("Dubai");
        updateRequest.setStartDateTime(LocalDateTime.now().plusDays(1));
        updateRequest.setEndDateTime(LocalDateTime.now().plusDays(2));
        updateRequest.setIsFree(true);
        updateRequest.setPrice(BigDecimal.ZERO);
        updateRequest.setOrganizerName("Test Organizer");
        updateRequest.setStatus(EventStatus.DRAFT);

        // 2. Setup User B (non-owner, non-admin)
        User userB = new User("userB@email.com", "User B", "user-b-google-id");
        userB.setRole(Role.USER);
        UserProfile profileB = new UserProfile();
        profileB.setUser(userB);
        profileB.setName("User B");
        userB.setProfile(profileB);
        userRepository.save(userB);

        when(googleTokenVerifier.verify("valid-user-b-token"))
                .thenReturn(new GoogleTokenClaims("user-b-google-id", "userB@email.com", "User B"));

        GoogleLoginRequest userBRequest = new GoogleLoginRequest("valid-user-b-token");
        MvcResult userBLoginResult = mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userBRequest)))
                .andReturn();
        String userBToken = JsonPath.read(userBLoginResult.getResponse().getContentAsString(), "$.token");

        // 3. User B attempts to update -> 403 Forbidden
        mockMvc.perform(put("/events/" + eventId)
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());

        // 4. ADMIN attempts to update -> 200 OK
        mockMvc.perform(put("/events/" + eventId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        // 5. User A (Owner) attempts to update -> 200 OK
        mockMvc.perform(put("/events/" + eventId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedRequestToProtectedEndpointReturnsUnauthorized() throws Exception {
        // No Authorization header provided -> 401
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/admin/dashboard/stats"))
                .andExpect(status().isUnauthorized());
    }
}
