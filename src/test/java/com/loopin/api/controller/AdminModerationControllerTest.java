package com.loopin.api.controller;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.moderation.enums.ContentModerationStatus;
import com.loopin.api.events.enums.EventCategory;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.events.enums.EventType;
import com.loopin.api.moderation.enums.ModerationAction;
import com.loopin.api.events.entity.Event;
import com.loopin.api.users.entity.User;
import com.loopin.api.events.repository.EventGroupRepository;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.moderation.repository.ModerationLogRepository;
import com.loopin.api.users.repository.UserRepository;
import com.loopin.api.common.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminModerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventGroupRepository eventGroupRepository;

    @Autowired
    private ModerationLogRepository moderationLogRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private User admin;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        moderationLogRepository.deleteAll();
        eventGroupRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        admin = userRepository.save(user("admin@loopin.test", Role.ADMIN));
        User regularUser = userRepository.save(user("user@loopin.test", Role.USER));
        adminToken = jwtUtils.generateToken(admin.getEmail(), admin.getRole().name());
        userToken = jwtUtils.generateToken(regularUser.getEmail(), regularUser.getRole().name());
    }

    @Test
    void getPendingItems_AdminCanListPendingContent() throws Exception {
        Event pending = pendingEvent("Pending event");
        eventRepository.save(pending);
        eventRepository.save(publishedEvent("Published event"));

        mockMvc.perform(get("/v1/admin/moderation/pending")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(1)))
                .andExpect(jsonPath("$.content[0].id", is(pending.getPublicId().toString())))
                .andExpect(jsonPath("$.content[0].moderationStatus", is("PENDING_REVIEW")));
    }

    @Test
    void approveEvent_AdminPublishesPendingContentAndWritesAuditLog() throws Exception {
        Event pending = eventRepository.save(pendingEvent("Approval candidate"));

        mockMvc.perform(patch("/v1/admin/moderation/events/" + pending.getPublicId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moderationStatus", is("APPROVED")));

        Event approved = eventRepository.findById(pending.getId()).orElseThrow();
        assertEquals(ContentModerationStatus.APPROVED, approved.getModerationStatus());
        assertEquals(EventStatus.PUBLISHED, approved.getStatus());
        assertEquals(1, moderationLogRepository.count());
        assertEquals(ModerationAction.APPROVED, moderationLogRepository.findAll().getFirst().getAction());
    }

    @Test
    void rejectEvent_AdminRejectsPendingContentStoresReasonAndWritesAuditLog() throws Exception {
        Event pending = eventRepository.save(pendingEvent("Rejection candidate"));

        mockMvc.perform(patch("/v1/admin/moderation/events/" + pending.getPublicId() + "/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Violates community guidelines\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moderationStatus", is("REJECTED")));

        Event rejected = eventRepository.findById(pending.getId()).orElseThrow();
        assertEquals(ContentModerationStatus.REJECTED, rejected.getModerationStatus());
        assertEquals(EventStatus.DRAFT, rejected.getStatus());
        assertEquals("Violates community guidelines", rejected.getModerationRejectionReason());
        assertEquals(ModerationAction.REJECTED, moderationLogRepository.findAll().getFirst().getAction());
        assertEquals("Violates community guidelines", moderationLogRepository.findAll().getFirst().getRejectionReason());
    }

    @Test
    void moderationEndpoints_RegularUserIsForbidden() throws Exception {
        Event pending = eventRepository.save(pendingEvent("Unauthorized candidate"));

        mockMvc.perform(get("/v1/admin/moderation/pending")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/v1/admin/moderation/events/" + pending.getPublicId() + "/approve")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void approveEvent_NonPendingContentReturnsConflictWithoutAuditLog() throws Exception {
        Event published = eventRepository.save(publishedEvent("Already published"));

        mockMvc.perform(patch("/v1/admin/moderation/events/" + published.getPublicId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());

        assertEquals(0, moderationLogRepository.count());
    }

    @Test
    void approveEvent_UnknownContentReturnsNotFoundWithoutAuditLog() throws Exception {
        mockMvc.perform(patch("/v1/admin/moderation/events/" + UUID.randomUUID() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());

        assertEquals(0, moderationLogRepository.count());
    }

    private Event pendingEvent(String title) {
        Event event = event(title);
        event.setStatus(EventStatus.DRAFT);
        event.setModerationStatus(ContentModerationStatus.PENDING_REVIEW);
        return event;
    }

    private Event publishedEvent(String title) {
        Event event = event(title);
        event.setStatus(EventStatus.PUBLISHED);
        event.setModerationStatus(ContentModerationStatus.APPROVED);
        return event;
    }

    private Event event(String title) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription("Description");
        event.setType(EventType.EVENT);
        event.setCategory(EventCategory.TECH);
        event.setCity("Baku");
        event.setStartDateTime(LocalDateTime.now().plusDays(1));
        event.setEndDateTime(LocalDateTime.now().plusDays(2));
        event.setIsFree(true);
        event.setOrganizerName("Loopin");
        return event;
    }

    private User user(String email, Role role) {
        User user = new User(email, email, null);
        user.setRole(role);
        return user;
    }
}
