package com.loopin.api.integration;

import com.jayway.jsonpath.JsonPath;
import com.loopin.api.auth.dto.GoogleLoginRequest;
import com.loopin.api.auth.enums.Role;
import com.loopin.api.auth.service.GoogleTokenClaims;
import com.loopin.api.auth.service.GoogleTokenVerifier;
import com.loopin.api.core.events.enums.EventCategory;
import com.loopin.api.core.events.enums.EventStatus;
import com.loopin.api.core.events.enums.EventType;
import com.loopin.api.core.groups.enums.GroupSizeType;
import com.loopin.api.core.groups.enums.GroupStatus;
import com.loopin.api.core.events.dto.request.EventCreateRequest;
import com.loopin.api.core.groups.dto.request.CreateGroupRequest;
import com.loopin.api.chat.dto.request.CreateGroupMessageRequest;
import com.loopin.api.core.events.entity.EventGroup;
import com.loopin.api.core.groups.entity.GroupMember;
import com.loopin.api.core.users.entity.User;
import com.loopin.api.core.users.entity.UserProfile;
import com.loopin.api.core.events.repository.EventGroupRepository;
import com.loopin.api.core.events.repository.EventRepository;
import com.loopin.api.core.groups.repository.GroupMemberRepository;
import com.loopin.api.chat.repository.GroupMessageRepository;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GroupMessagingPersistenceIntegrationTest extends AbstractIntegrationTest {

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
    private GroupMessageRepository groupMessageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GoogleTokenVerifier googleTokenVerifier;

    private String user1Token; // Member 1
    private String user2Token; // Member 2
    private String nonMemberToken; // Non-member

    private User user1;
    private User user2;
    private User nonMember;

    private String eventId;
    private EventGroup group;

    @BeforeEach
    void setUp() throws Exception {
        reset(googleTokenVerifier);
        groupMessageRepository.deleteAll();
        groupMemberRepository.deleteAll();
        eventGroupRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        // User 1 (will create group and be member)
        user1 = new User("user1@email.com", "User 1", "user1-google-id");
        user1.setRole(Role.USER);
        UserProfile p1 = new UserProfile();
        p1.setUser(user1);
        p1.setName("User 1");
        user1.setProfile(p1);
        user1 = userRepository.save(user1);

        // User 2 (will be added as member)
        user2 = new User("user2@email.com", "User 2", "user2-google-id");
        user2.setRole(Role.USER);
        UserProfile p2 = new UserProfile();
        p2.setUser(user2);
        p2.setName("User 2");
        user2.setProfile(p2);
        user2 = userRepository.save(user2);

        // Non Member
        nonMember = new User("nonmember@email.com", "Non Member", "nonmember-google-id");
        nonMember.setRole(Role.USER);
        UserProfile p3 = new UserProfile();
        p3.setUser(nonMember);
        p3.setName("Non Member");
        nonMember.setProfile(p3);
        nonMember = userRepository.save(nonMember);

        // Mock verification
        when(googleTokenVerifier.verify("valid-user1-token"))
                .thenReturn(new GoogleTokenClaims("user1-google-id", "user1@email.com", "User 1"));
        when(googleTokenVerifier.verify("valid-user2-token"))
                .thenReturn(new GoogleTokenClaims("user2-google-id", "user2@email.com", "User 2"));
        when(googleTokenVerifier.verify("valid-nonmember-token"))
                .thenReturn(new GoogleTokenClaims("nonmember-google-id", "nonmember@email.com", "Non Member"));

        // Obtain JWTs
        user1Token = getToken("valid-user1-token");
        user2Token = getToken("valid-user2-token");
        nonMemberToken = getToken("valid-nonmember-token");

        // Create Event
        EventCreateRequest createEventRequest = new EventCreateRequest();
        createEventRequest.setTitle("Messaging Event");
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
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventRequest)))
                .andReturn();
        eventId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        // Create Group
        CreateGroupRequest request = new CreateGroupRequest();
        request.setEventId(UUID.fromString(eventId));
        request.setTitle("Chat Group");
        request.setGroupSize(GroupSizeType.FOUR);
        request.setMaxMembers(4);

        mockMvc.perform(post("/groups")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
        
        // Setup group and members for the message test (since message API uses Long ID)
        group = eventGroupRepository.findAll().get(0);

        GroupMember m2 = new GroupMember();
        m2.setGroup(group);
        m2.setUser(user2);
        groupMemberRepository.save(m2);
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
    void sendMessage_ExistingMember_ReturnsCreatedAndPersists() throws Exception {
        CreateGroupMessageRequest req = new CreateGroupMessageRequest();
        req.setMessageText("Hello group!");

        long initialCount = groupMessageRepository.count();

        mockMvc.perform(post("/groups/" + group.getPublicId() + "/messages")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.messageText", is("Hello group!")))
                .andExpect(jsonPath("$.senderId", is(user1.getPublicId().toString()))); // Note: JSONPath reads Longs as Ints sometimes

        assertEquals(initialCount + 1, groupMessageRepository.count());
    }

    @Test
    void sendMessage_NonMember_ReturnsConflict() throws Exception {
        CreateGroupMessageRequest req = new CreateGroupMessageRequest();
        req.setMessageText("I am not a member");

        mockMvc.perform(post("/groups/" + group.getPublicId() + "/messages")
                        .header("Authorization", "Bearer " + nonMemberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Only group members can access group messages")));
    }

    @Test
    void getMessages_MemberRequests_ReturnsMessagesInOrder() throws Exception {
        // Send two messages
        CreateGroupMessageRequest req1 = new CreateGroupMessageRequest();
        req1.setMessageText("First message");
        mockMvc.perform(post("/groups/" + group.getPublicId() + "/messages")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)));

        CreateGroupMessageRequest req2 = new CreateGroupMessageRequest();
        req2.setMessageText("Second message");
        mockMvc.perform(post("/groups/" + group.getPublicId() + "/messages")
                        .header("Authorization", "Bearer " + user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)));

        mockMvc.perform(get("/groups/" + group.getPublicId() + "/messages")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(2)))
                .andExpect(jsonPath("$.content[0].messageText", is("First message")))
                .andExpect(jsonPath("$.content[0].senderName", is("User 1")))
                .andExpect(jsonPath("$.content[1].messageText", is("Second message")))
                .andExpect(jsonPath("$.content[1].senderName", is("User 2")));
    }

    @Test
    void getMessages_NonMemberRequests_ReturnsConflict() throws Exception {
        mockMvc.perform(get("/groups/" + group.getPublicId() + "/messages")
                        .header("Authorization", "Bearer " + nonMemberToken))
                .andExpect(status().isConflict());
    }

    @Test
    void sendMessage_ArchivedGroup_ReturnsConflict() throws Exception {
        group.setStatus(GroupStatus.ARCHIVED);
        eventGroupRepository.save(group);

        CreateGroupMessageRequest req = new CreateGroupMessageRequest();
        req.setMessageText("Trying to message archived group");

        mockMvc.perform(post("/groups/" + group.getPublicId() + "/messages")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("no longer allows messaging")));
    }

    @Test
    void multipleMessagePersistenceSurvivesAndPreservesOrder() throws Exception {
        CreateGroupMessageRequest req1 = new CreateGroupMessageRequest();
        req1.setMessageText("MSG 1");
        mockMvc.perform(post("/groups/" + group.getPublicId() + "/messages")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1)));

        CreateGroupMessageRequest req2 = new CreateGroupMessageRequest();
        req2.setMessageText("MSG 2");
        mockMvc.perform(post("/groups/" + group.getPublicId() + "/messages")
                .header("Authorization", "Bearer " + user2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2)));

        CreateGroupMessageRequest req3 = new CreateGroupMessageRequest();
        req3.setMessageText("MSG 3");
        mockMvc.perform(post("/groups/" + group.getPublicId() + "/messages")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req3)));

        mockMvc.perform(get("/groups/" + group.getPublicId() + "/messages")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(3)))
                .andExpect(jsonPath("$.content[0].messageText", is("MSG 1")))
                .andExpect(jsonPath("$.content[1].messageText", is("MSG 2")))
                .andExpect(jsonPath("$.content[2].messageText", is("MSG 3")));
    }
}
