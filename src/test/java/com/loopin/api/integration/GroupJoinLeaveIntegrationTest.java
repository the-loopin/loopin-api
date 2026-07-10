package com.loopin.api.integration;

import com.jayway.jsonpath.JsonPath;
import com.loopin.api.auth.dto.GoogleLoginRequest;
import com.loopin.api.auth.enums.Role;
import com.loopin.api.auth.service.GoogleTokenClaims;
import com.loopin.api.auth.service.GoogleTokenVerifier;
import com.loopin.api.events.enums.EventCategory;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.events.enums.EventType;
import com.loopin.api.groups.enums.GroupSizeType;
import com.loopin.api.groups.enums.GroupStatus;
import com.loopin.api.groups.enums.RequestStatus;
import com.loopin.api.events.dto.request.EventCreateRequest;
import com.loopin.api.groups.dto.request.CreateGroupJoinRequestRequest;
import com.loopin.api.groups.dto.request.CreateGroupRequest;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.entity.UserProfile;
import com.loopin.api.events.repository.EventGroupRepository;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.groups.repository.GroupJoinRequestRepository;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.users.repository.UserRepository;
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

class GroupJoinLeaveIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventGroupRepository eventGroupRepository;

    @Autowired
    private GroupJoinRequestRepository groupJoinRequestRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GoogleTokenVerifier googleTokenVerifier;

    private String adminToken; // Group creator/admin
    private String userToken; // Regular user
    private String user2Token; // Another regular user

    private User adminUser;
    private User testUser;
    private User testUser2;

    private String eventId;

    @BeforeEach
    void setUp() throws Exception {
        reset(googleTokenVerifier);
        groupMemberRepository.deleteAll();
        groupJoinRequestRepository.deleteAll();
        eventGroupRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        // Admin User (will create the group)
        adminUser = new User("admin@email.com", "Admin User", "admin-google-id");
        adminUser.setRole(Role.USER); // Doesn't need to be global ADMIN to be group admin
        UserProfile adminProfile = new UserProfile();
        adminProfile.setUser(adminUser);
        adminProfile.setName("Admin User");
        adminUser.setProfile(adminProfile);
        adminUser = userRepository.save(adminUser);

        // Regular User 1
        testUser = new User("user@email.com", "Test User", "user-google-id");
        testUser.setRole(Role.USER);
        UserProfile userProfile = new UserProfile();
        userProfile.setUser(testUser);
        userProfile.setName("Test User");
        testUser.setProfile(userProfile);
        testUser = userRepository.save(testUser);

        // Regular User 2
        testUser2 = new User("user2@email.com", "Test User 2", "user2-google-id");
        testUser2.setRole(Role.USER);
        UserProfile userProfile2 = new UserProfile();
        userProfile2.setUser(testUser2);
        userProfile2.setName("Test User 2");
        testUser2.setProfile(userProfile2);
        testUser2 = userRepository.save(testUser2);

        // Mock verification
        when(googleTokenVerifier.verify("valid-admin-token"))
                .thenReturn(new GoogleTokenClaims("admin-google-id", "admin@email.com", "Admin User"));
        when(googleTokenVerifier.verify("valid-user-token"))
                .thenReturn(new GoogleTokenClaims("user-google-id", "user@email.com", "Test User"));
        when(googleTokenVerifier.verify("valid-user2-token"))
                .thenReturn(new GoogleTokenClaims("user2-google-id", "user2@email.com", "Test User 2"));

        // Obtain JWTs
        adminToken = getToken("valid-admin-token");
        userToken = getToken("valid-user-token");
        user2Token = getToken("valid-user2-token");

        // Create an event for groups to attach to
        EventCreateRequest createEventRequest = new EventCreateRequest();
        createEventRequest.setTitle("Group Test Event");
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

        MvcResult result = mockMvc.perform(post("/v1/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        eventId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String getToken(String mockToken) throws Exception {
        GoogleLoginRequest req = new GoogleLoginRequest(mockToken);
        MvcResult res = mockMvc.perform(post("/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.token");
    }

    private String createGroup(String title, GroupSizeType sizeType) throws Exception {
        CreateGroupRequest request = new CreateGroupRequest();
        request.setEventId(UUID.fromString(eventId));
        request.setTitle(title);
        request.setGroupSize(sizeType);
        request.setMaxMembers(sizeType.getMaxMembers());
        request.setGroupNote("Note");

        MvcResult result = mockMvc.perform(post("/v1/groups")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    @Test
    void createGroup_ValidRequest_ReturnsCreatedAndSavesToDb() throws Exception {
        long initialCount = eventGroupRepository.count();
        long initialMemberCount = groupMemberRepository.count();

        String groupId = createGroup("Test Group", GroupSizeType.FOUR);

        assertEquals(initialCount + 1, eventGroupRepository.count());
        // The creator is automatically added as a member
        assertEquals(initialMemberCount + 1, groupMemberRepository.count());

        mockMvc.perform(get("/v1/groups/" + groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Test Group")))
                .andExpect(jsonPath("$.status", is("OPEN")));
    }

    @Test
    void sendJoinRequest_ValidRequest_ReturnsPendingStatus() throws Exception {
        String groupId = createGroup("Test Group", GroupSizeType.FOUR);

        CreateGroupJoinRequestRequest joinRequest = new CreateGroupJoinRequestRequest();
        joinRequest.setMessage("Let me in!");

        mockMvc.perform(post("/v1/groups/" + groupId + "/join-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.message", is("Let me in!")));

        assertEquals(1, groupJoinRequestRepository.count());
    }

    @Test
    void sendJoinRequest_DuplicateRequest_ReturnsConflict() throws Exception {
        String groupId = createGroup("Test Group", GroupSizeType.FOUR);

        CreateGroupJoinRequestRequest joinRequest = new CreateGroupJoinRequestRequest();
        joinRequest.setMessage("First request");

        // First request succeeds
        mockMvc.perform(post("/v1/groups/" + groupId + "/join-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().isCreated());

        // Second request from same user fails with 409 Conflict
        mockMvc.perform(post("/v1/groups/" + groupId + "/join-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void approveJoinRequest_AdminApproves_CreatesGroupMemberRow() throws Exception {
        String groupId = createGroup("Test Group", GroupSizeType.FOUR);

        CreateGroupJoinRequestRequest joinRequest = new CreateGroupJoinRequestRequest();
        joinRequest.setMessage("Let me in!");

        MvcResult joinResult = mockMvc.perform(post("/v1/groups/" + groupId + "/join-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andReturn();
        String requestId = JsonPath.read(joinResult.getResponse().getContentAsString(), "$.id");

        long initialMemberCount = groupMemberRepository.count();

        // Admin approves request
        mockMvc.perform(patch("/v1/groups/" + groupId + "/join-requests/" + requestId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACCEPTED")));

        // Verifying member is added (+1)
        assertEquals(initialMemberCount + 1, groupMemberRepository.count());
    }

    @Test
    void rejectJoinRequest_AdminRejects_DoesNotCreateGroupMember() throws Exception {
        String groupId = createGroup("Test Group", GroupSizeType.FOUR);

        CreateGroupJoinRequestRequest joinRequest = new CreateGroupJoinRequestRequest();
        joinRequest.setMessage("Let me in!");

        MvcResult joinResult = mockMvc.perform(post("/v1/groups/" + groupId + "/join-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andReturn();
        String requestId = JsonPath.read(joinResult.getResponse().getContentAsString(), "$.id");

        long initialMemberCount = groupMemberRepository.count();

        // Admin rejects request
        mockMvc.perform(patch("/v1/groups/" + groupId + "/join-requests/" + requestId + "/reject")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")));

        // Verifying member is NOT added
        assertEquals(initialMemberCount, groupMemberRepository.count());
    }

    @Test
    void approveJoinRequest_NonAdmin_ReturnsForbidden() throws Exception {
        String groupId = createGroup("Test Group", GroupSizeType.FOUR);

        CreateGroupJoinRequestRequest joinRequest = new CreateGroupJoinRequestRequest();
        joinRequest.setMessage("Let me in!");

        MvcResult joinResult = mockMvc.perform(post("/v1/groups/" + groupId + "/join-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andReturn();
        String requestId = JsonPath.read(joinResult.getResponse().getContentAsString(), "$.id");

        // Non-admin (User 2) tries to approve -> 403
        mockMvc.perform(patch("/v1/groups/" + groupId + "/join-requests/" + requestId + "/approve")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void groupReachesCapacity_ApprovingRequest_ChangesStatusToFull() throws Exception {
        // Create a group that allows exactly 2 members
        String groupId = createGroup("Test Group", GroupSizeType.TWO);

        CreateGroupJoinRequestRequest joinRequest = new CreateGroupJoinRequestRequest();
        joinRequest.setMessage("Let me in!");

        MvcResult joinResult = mockMvc.perform(post("/v1/groups/" + groupId + "/join-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andReturn();
        String requestId = JsonPath.read(joinResult.getResponse().getContentAsString(), "$.id");

        // Admin approves request (this makes it 2 members: admin + userToken)
        mockMvc.perform(patch("/v1/groups/" + groupId + "/join-requests/" + requestId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Verify group status is now FULL
        mockMvc.perform(get("/v1/groups/" + groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("FULL")));
    }

    @Test
    void joinRequestToFullGroup_ReturnsConflict() throws Exception {
        // Group size 2
        String groupId = createGroup("Test Group", GroupSizeType.TWO);

        // First user joins and is approved (group becomes FULL)
        CreateGroupJoinRequestRequest joinRequest = new CreateGroupJoinRequestRequest();
        joinRequest.setMessage("Let me in!");
        MvcResult joinResult = mockMvc.perform(post("/v1/groups/" + groupId + "/join-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andReturn();
        String requestId = JsonPath.read(joinResult.getResponse().getContentAsString(), "$.id");
        mockMvc.perform(patch("/v1/groups/" + groupId + "/join-requests/" + requestId + "/approve")
                        .header("Authorization", "Bearer " + adminToken));

        // Second user tries to join FULL group -> 409 Conflict
        CreateGroupJoinRequestRequest joinRequest2 = new CreateGroupJoinRequestRequest();
        joinRequest2.setMessage("Me too!");
        mockMvc.perform(post("/v1/groups/" + groupId + "/join-requests")
                        .header("Authorization", "Bearer " + user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest2)))
                .andExpect(status().isConflict());
    }

    @Test
    void removeMember_MemberAttemptsToRemoveSelf_ReturnsForbiddenAsThereIsNoDedicatedLeaveEndpoint() throws Exception {
        String groupId = createGroup("Test Group", GroupSizeType.FOUR);

        // Add user token as a member first via admin approval
        CreateGroupJoinRequestRequest joinRequest = new CreateGroupJoinRequestRequest();
        joinRequest.setMessage("Let me in!");
        MvcResult joinResult = mockMvc.perform(post("/v1/groups/" + groupId + "/join-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andReturn();
        String requestId = JsonPath.read(joinResult.getResponse().getContentAsString(), "$.id");
        mockMvc.perform(patch("/v1/groups/" + groupId + "/join-requests/" + requestId + "/approve")
                        .header("Authorization", "Bearer " + adminToken));

        // The user tries to leave by calling DELETE /groups/{groupId}/members/{userId}
        // This fails with 403 Forbidden because GroupServiceImpl.validateGroupAdmin only allows admins
        mockMvc.perform(delete("/v1/groups/" + groupId + "/members/" + testUser.getPublicId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void removeMember_AdminRemovesMember_ReturnsNoContentAndMemberRemoved() throws Exception {
        String groupId = createGroup("Test Group", GroupSizeType.FOUR);

        // Add user
        CreateGroupJoinRequestRequest joinRequest = new CreateGroupJoinRequestRequest();
        joinRequest.setMessage("Let me in!");
        MvcResult joinResult = mockMvc.perform(post("/v1/groups/" + groupId + "/join-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andReturn();
        String requestId = JsonPath.read(joinResult.getResponse().getContentAsString(), "$.id");
        mockMvc.perform(patch("/v1/groups/" + groupId + "/join-requests/" + requestId + "/approve")
                        .header("Authorization", "Bearer " + adminToken));

        // Admin removes the user
        mockMvc.perform(delete("/v1/groups/" + groupId + "/members/" + testUser.getPublicId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Check user is removed via GET /members
        mockMvc.perform(get("/v1/groups/" + groupId + "/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1))); // only admin is left
    }
}
