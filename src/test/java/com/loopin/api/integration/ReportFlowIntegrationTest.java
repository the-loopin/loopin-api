package com.loopin.api.integration;

import com.jayway.jsonpath.JsonPath;
import com.loopin.api.auth.dto.GoogleLoginRequest;
import com.loopin.api.auth.enums.Role;
import com.loopin.api.auth.service.GoogleTokenClaims;
import com.loopin.api.auth.service.GoogleTokenVerifier;
import com.loopin.api.common.enums.EventCategory;
import com.loopin.api.common.enums.EventStatus;
import com.loopin.api.common.enums.EventType;
import com.loopin.api.common.enums.GroupSizeType;
import com.loopin.api.common.enums.ReportStatus;
import com.loopin.api.common.enums.ReportTargetType;
import com.loopin.api.dto.event.request.EventCreateRequest;
import com.loopin.api.dto.group.request.CreateGroupRequest;
import com.loopin.api.dto.report.request.CreateReportRequest;
import com.loopin.api.dto.report.request.UpdateReportStatusRequest;
import com.loopin.api.entity.User;
import com.loopin.api.entity.UserProfile;
import com.loopin.api.repository.EventGroupRepository;
import com.loopin.api.repository.EventRepository;
import com.loopin.api.repository.GroupMemberRepository;
import com.loopin.api.repository.UserReportRepository;
import com.loopin.api.repository.UserRepository;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventGroupRepository eventGroupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private UserReportRepository userReportRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GoogleTokenVerifier googleTokenVerifier;

    private String adminToken; // Global ADMIN
    private String userToken; // Regular user

    private User adminUser;
    private User testUser;

    private String eventId;
    private String groupId;

    @BeforeEach
    void setUp() throws Exception {
        reset(googleTokenVerifier);
        userReportRepository.deleteAll();
        groupMemberRepository.deleteAll();
        eventGroupRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        // Admin User (Global)
        adminUser = new User("admin@email.com", "Admin User", "admin-google-id");
        adminUser.setRole(Role.ADMIN);
        UserProfile adminProfile = new UserProfile();
        adminProfile.setUser(adminUser);
        adminProfile.setName("Admin User");
        adminUser.setProfile(adminProfile);
        adminUser = userRepository.save(adminUser);

        // Regular User
        testUser = new User("user@email.com", "Test User", "user-google-id");
        testUser.setRole(Role.USER);
        UserProfile userProfile = new UserProfile();
        userProfile.setUser(testUser);
        userProfile.setName("Test User");
        testUser.setProfile(userProfile);
        testUser = userRepository.save(testUser);

        // Mock verification
        when(googleTokenVerifier.verify("valid-admin-token"))
                .thenReturn(new GoogleTokenClaims("admin-google-id", "admin@email.com", "Admin User"));
        when(googleTokenVerifier.verify("valid-user-token"))
                .thenReturn(new GoogleTokenClaims("user-google-id", "user@email.com", "Test User"));

        // Obtain JWTs
        adminToken = getToken("valid-admin-token");
        userToken = getToken("valid-user-token");

        // Create an event for groups to attach to
        EventCreateRequest createEventRequest = new EventCreateRequest();
        createEventRequest.setTitle("Report Test Event");
        createEventRequest.setDescription("Event Description");
        createEventRequest.setType(EventType.EVENT);
        createEventRequest.setCategory(EventCategory.TECH);
        createEventRequest.setCity("Dubai");
        createEventRequest.setStartDateTime(LocalDateTime.now().plusDays(1).withNano(0));
        createEventRequest.setEndDateTime(LocalDateTime.now().plusDays(2).withNano(0));
        createEventRequest.setIsFree(true);
        createEventRequest.setPrice(BigDecimal.ZERO);
        createEventRequest.setOrganizerName("Tech Hub");
        createEventRequest.setStatus(EventStatus.PUBLISHED);

        MvcResult result = mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventRequest)))
                .andReturn();

        eventId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        // Create a Group to report
        CreateGroupRequest request = new CreateGroupRequest();
        request.setEventId(UUID.fromString(eventId));
        request.setTitle("Group to Report");
        request.setGroupSize(GroupSizeType.FOUR);
        request.setMaxMembers(4);

        MvcResult groupResult = mockMvc.perform(post("/groups")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        groupId = JsonPath.read(groupResult.getResponse().getContentAsString(), "$.id");
    }

    private String getToken(String mockToken) throws Exception {
        GoogleLoginRequest req = new GoogleLoginRequest(mockToken);
        MvcResult res = mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.token");
    }

    @Test
    void submitReport_ValidRequest_CreatesReportAsPending() throws Exception {
        CreateReportRequest req = new CreateReportRequest();
        req.setTargetType(ReportTargetType.GROUP);
        req.setTargetId(UUID.fromString(groupId));
        req.setReason("Inappropriate Content");
        req.setDetails("This group contains bad words.");

        long initialCount = userReportRepository.count();

        mockMvc.perform(post("/reports")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.reason", is("Inappropriate Content")));

        assertEquals(initialCount + 1, userReportRepository.count());
    }

    @Test
    void listReports_AdminRole_ReturnsReports() throws Exception {
        // User creates a report
        CreateReportRequest req = new CreateReportRequest();
        req.setTargetType(ReportTargetType.GROUP);
        req.setTargetId(UUID.fromString(groupId));
        req.setReason("Spam");
        req.setDetails("Too many messages");

        mockMvc.perform(post("/reports")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)));

        // Admin lists reports
        mockMvc.perform(get("/admin/reports")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(1)))
                .andExpect(jsonPath("$.content[0].reason", is("Spam")))
                .andExpect(jsonPath("$.content[0].status", is("PENDING")));
    }

    @Test
    void listReports_UserRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/admin/reports")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateReportStatus_AdminRole_UpdatesSuccessfully() throws Exception {
        // User creates a report
        CreateReportRequest req = new CreateReportRequest();
        req.setTargetType(ReportTargetType.GROUP);
        req.setTargetId(UUID.fromString(groupId));
        req.setReason("Harassment");
        req.setDetails("User is harassing me");

        MvcResult createResult = mockMvc.perform(post("/reports")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn();

        String reportId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        // Admin updates status
        UpdateReportStatusRequest updateReq = new UpdateReportStatusRequest();
        updateReq.setStatus(ReportStatus.RESOLVED);

        mockMvc.perform(patch("/admin/reports/" + reportId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RESOLVED")));

        // Verify with get
        mockMvc.perform(get("/admin/reports")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status", is("RESOLVED")));
    }
}
