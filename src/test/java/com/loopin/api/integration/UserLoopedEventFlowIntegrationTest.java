package com.loopin.api.integration;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.common.security.JwtUtils;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.enums.EventCategory;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.events.enums.EventType;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.events.repository.UserLoopedEventRepository;
import com.loopin.api.moderation.enums.ContentModerationStatus;
import com.loopin.api.notifications.repository.NotificationRepository;
import com.loopin.api.support.AbstractIntegrationTest;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserLoopedEventFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserLoopedEventRepository loopedEventRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private User owner;
    private User participant;
    private Event event;

    private String ownerToken;
    private String participantToken;

    @BeforeEach
    void setUp() {
        owner = saveUser(
            "owner@loopin.test",
            "Event Owner"
        );

        participant = saveUser(
            "participant@loopin.test",
            "Participant"
        );

        ownerToken = jwtUtils.generateToken(
            owner.getEmail(),
            owner.getRole().name()
        );

        participantToken = jwtUtils.generateToken(
            participant.getEmail(),
            participant.getRole().name()
        );

        event = saveEvent(
            "Loopin Integration Event",
            owner
        );
    }

    @Test
    void loopIn_RepeatedRequestIsIdempotent_AndCanBeRemoved()
        throws Exception {

        String notificationDeduplicationKey =
            "event-loop-in:"
                + event.getPublicId()
                + ":"
                + participant.getPublicId();

        /*
         * First loop-in:
         * - relation is created;
         * - count becomes 1;
         * - owner notification is created.
         */
        mockMvc.perform(
                post("/v1/events/{eventId}/loop-in",
                    event.getPublicId())
                    .header(
                        "Authorization",
                        bearer(participantToken)
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath(
                "$.event.id",
                is(event.getPublicId().toString())
            ))
            .andExpect(jsonPath(
                "$.event.title",
                is("Loopin Integration Event")
            ))
            .andExpect(jsonPath(
                "$.loopedCount",
                is(1)
            ));

        assertTrue(
            loopedEventRepository.existsByUserIdAndEventId(
                participant.getId(),
                event.getId()
            )
        );

        assertEquals(
            1L,
            loopedEventRepository.countByEventId(event.getId())
        );

        assertTrue(
            notificationRepository.existsByDeduplicationKey(
                notificationDeduplicationKey
            )
        );

        assertEquals(
            1L,
            notificationRepository.count()
        );

        /*
         * Repeated loop-in:
         * - no duplicate relation;
         * - count remains 1;
         * - no duplicate notification.
         */
        mockMvc.perform(
                post("/v1/events/{eventId}/loop-in",
                    event.getPublicId())
                    .header(
                        "Authorization",
                        bearer(participantToken)
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath(
                "$.loopedCount",
                is(1)
            ));

        assertEquals(
            1L,
            loopedEventRepository.countByEventId(event.getId())
        );

        assertEquals(
            1L,
            notificationRepository.count()
        );

        /*
         * Current user's looped-events listing.
         */
        mockMvc.perform(
                get("/v1/me/looped-events")
                    .header(
                        "Authorization",
                        bearer(participantToken)
                    )
                    .param("page", "0")
                    .param("size", "10")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath(
                "$.content.length()",
                is(1)
            ))
            .andExpect(jsonPath(
                "$.content[0].event.id",
                is(event.getPublicId().toString())
            ))
            .andExpect(jsonPath(
                "$.content[0].loopedCount",
                is(1)
            ))
            .andExpect(jsonPath(
                "$.totalElements",
                is(1)
            ));

        /*
         * First removal deletes the relation.
         */
        mockMvc.perform(
                delete("/v1/events/{eventId}/loop-in",
                    event.getPublicId())
                    .header(
                        "Authorization",
                        bearer(participantToken)
                    )
            )
            .andExpect(status().isNoContent());

        assertFalse(
            loopedEventRepository.existsByUserIdAndEventId(
                participant.getId(),
                event.getId()
            )
        );

        assertEquals(
            0L,
            loopedEventRepository.countByEventId(event.getId())
        );

        /*
         * Repeated removal remains idempotent.
         */
        mockMvc.perform(
                delete("/v1/events/{eventId}/loop-in",
                    event.getPublicId())
                    .header(
                        "Authorization",
                        bearer(participantToken)
                    )
            )
            .andExpect(status().isNoContent());

        /*
         * Listing becomes empty after removal.
         */
        mockMvc.perform(
                get("/v1/me/looped-events")
                    .header(
                        "Authorization",
                        bearer(participantToken)
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath(
                "$.content.length()",
                is(0)
            ))
            .andExpect(jsonPath(
                "$.totalElements",
                is(0)
            ));
    }

    @Test
    void loopIn_UnauthenticatedRequest_ReturnsUnauthorized()
        throws Exception {

        mockMvc.perform(
                post("/v1/events/{eventId}/loop-in",
                    event.getPublicId())
            )
            .andExpect(status().isUnauthorized());

        assertEquals(
            0L,
            loopedEventRepository.countByEventId(event.getId())
        );

        assertEquals(
            0L,
            notificationRepository.count()
        );
    }

    @Test
    void getMyLoopedEvents_UnauthenticatedRequest_ReturnsUnauthorized()
        throws Exception {

        mockMvc.perform(
                get("/v1/me/looped-events")
            )
            .andExpect(status().isUnauthorized());
    }

    @Test
    void removeLoopIn_UnauthenticatedRequest_ReturnsUnauthorized()
        throws Exception {

        mockMvc.perform(
                delete("/v1/events/{eventId}/loop-in",
                    event.getPublicId())
            )
            .andExpect(status().isUnauthorized());
    }

    @Test
    void loopIn_CancelledEvent_ReturnsConflictAndDoesNotPersist()
        throws Exception {

        event.setStatus(EventStatus.CANCELLED);
        eventRepository.saveAndFlush(event);

        mockMvc.perform(
                post("/v1/events/{eventId}/loop-in",
                    event.getPublicId())
                    .header(
                        "Authorization",
                        bearer(participantToken)
                    )
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status", is(409)));

        assertEquals(
            0L,
            loopedEventRepository.countByEventId(event.getId())
        );

        assertEquals(
            0L,
            notificationRepository.count()
        );
    }

    @Test
    void loopIn_ExpiredEvent_ReturnsConflictAndDoesNotPersist()
        throws Exception {

        event.setStartDateTime(
            LocalDateTime.now().minusDays(2)
        );
        event.setEndDateTime(
            LocalDateTime.now().minusDays(1)
        );

        eventRepository.saveAndFlush(event);

        mockMvc.perform(
                post("/v1/events/{eventId}/loop-in",
                    event.getPublicId())
                    .header(
                        "Authorization",
                        bearer(participantToken)
                    )
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status", is(409)));

        assertEquals(
            0L,
            loopedEventRepository.countByEventId(event.getId())
        );
    }

    @Test
    void loopIn_RejectedModerationEvent_ReturnsConflict()
        throws Exception {

        event.setModerationStatus(
            ContentModerationStatus.REJECTED
        );

        eventRepository.saveAndFlush(event);

        mockMvc.perform(
                post("/v1/events/{eventId}/loop-in",
                    event.getPublicId())
                    .header(
                        "Authorization",
                        bearer(participantToken)
                    )
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status", is(409)));

        assertEquals(
            0L,
            loopedEventRepository.countByEventId(event.getId())
        );
    }

    @Test
    void loopIn_OwnerLoopsIntoOwnEvent_DoesNotCreateNotification()
        throws Exception {

        mockMvc.perform(
                post("/v1/events/{eventId}/loop-in",
                    event.getPublicId())
                    .header(
                        "Authorization",
                        bearer(ownerToken)
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath(
                "$.loopedCount",
                is(1)
            ));

        assertTrue(
            loopedEventRepository.existsByUserIdAndEventId(
                owner.getId(),
                event.getId()
            )
        );

        assertEquals(
            0L,
            notificationRepository.count()
        );
    }

    @Test
    void loopIn_NonexistentEvent_ReturnsNotFound()
        throws Exception {

        UUID missingEventId = UUID.randomUUID();

        mockMvc.perform(
                post("/v1/events/{eventId}/loop-in",
                    missingEventId)
                    .header(
                        "Authorization",
                        bearer(participantToken)
                    )
            )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status", is(404)));

        assertEquals(
            0L,
            loopedEventRepository.count()
        );
    }

    private User saveUser(
        String email,
        String name
    ) {
        User user = new User(
            email,
            name,
            null
        );

        user.setRole(Role.USER);
        user.setIsActive(true);

        return userRepository.saveAndFlush(user);
    }

    private Event saveEvent(
        String title,
        User eventOwner
    ) {
        Event savedEvent = new Event();

        savedEvent.setTitle(title);
        savedEvent.setDescription(
            "Integration test event description"
        );

        savedEvent.setType(EventType.EVENT);
        savedEvent.setCategory(EventCategory.TECH);

        savedEvent.setCity("Baku");
        savedEvent.setAddress("Baku Convention Center");

        savedEvent.setLatitude(
            new BigDecimal("40.409264")
        );
        savedEvent.setLongitude(
            new BigDecimal("49.867092")
        );

        savedEvent.setStartDateTime(
            LocalDateTime.now()
                .plusDays(1)
                .withNano(0)
        );

        savedEvent.setEndDateTime(
            LocalDateTime.now()
                .plusDays(2)
                .withNano(0)
        );

        savedEvent.setIsFree(true);
        savedEvent.setPrice(BigDecimal.ZERO);

        savedEvent.setOrganizerName("Loopin");
        savedEvent.setImageUrl(
            "https://example.test/event.jpg"
        );

        savedEvent.setStatus(EventStatus.PUBLISHED);
        savedEvent.setModerationStatus(
            ContentModerationStatus.APPROVED
        );

        savedEvent.setOwner(eventOwner);

        return eventRepository.saveAndFlush(savedEvent);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
