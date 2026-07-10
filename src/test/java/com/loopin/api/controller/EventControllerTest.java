package com.loopin.api.controller;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.events.enums.EventCategory;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.events.enums.EventType;
import com.loopin.api.common.security.JwtUtils;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.entity.EventInterest;
import com.loopin.api.interests.entity.Interest;
import com.loopin.api.users.entity.User;
import com.loopin.api.events.repository.EventInterestRepository;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.interests.repository.InterestRepository;
import com.loopin.api.users.repository.UserRepository;
import com.loopin.api.events.service.EventService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@Transactional
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private InterestRepository interestRepository;

    @Autowired
    private EventInterestRepository eventInterestRepository;

    @Autowired
    private EventService eventService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private User owner;
    private User otherUser;
    private User admin;
    private String ownerToken;
    private String otherUserToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        eventInterestRepository.deleteAll();
        eventRepository.deleteAll();
        interestRepository.deleteAll();
        userRepository.deleteAll();

        owner = saveUser("owner@email.com", "Owner", Role.USER);
        otherUser = saveUser("other@email.com", "Other User", Role.USER);
        admin = saveUser("admin@email.com", "Admin", Role.ADMIN);

        ownerToken = jwtUtils.generateToken(owner.getEmail(), owner.getRole().name());
        otherUserToken = jwtUtils.generateToken(otherUser.getEmail(), otherUser.getRole().name());
        adminToken = jwtUtils.generateToken(admin.getEmail(), admin.getRole().name());
    }

    @Test
    void createEvent_AssignsAuthenticatedUserAsOwner() throws Exception {
        mockMvc.perform(post("/v1/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventPayload("Owned Event")))
                .andExpect(status().isCreated());

        Event event = eventRepository.findAll().get(0);
        assertEquals(owner.getId(), event.getOwner().getId());
    }

    @Test
    void createEvent_AssignsInterests() throws Exception {
        Interest tech = interestRepository.save(interest("Tech", "tech", "Professional"));
        Interest music = interestRepository.save(interest("Music", "music", "Culture"));

        mockMvc.perform(post("/v1/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventPayload("Event With Interests", tech, music)))
                .andExpect(status().isCreated());

        Event event = eventRepository.findAll().get(0);
        assertEquals(2, eventInterestRepository.findByEvent_Id(event.getId()).size());
    }

    @Test
    void updateEvent_NonOwnerIsForbidden() throws Exception {
        Event event = eventRepository.save(event("Owned Event", owner));

        mockMvc.perform(put("/v1/events/" + event.getPublicId())
                        .header("Authorization", "Bearer " + otherUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventPayload("Hijacked Event")))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateEvent_AdminCanUpdateAnyEvent() throws Exception {
        Event event = eventRepository.save(event("Owned Event", owner));

        mockMvc.perform(put("/v1/events/" + event.getPublicId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventPayload("Admin Updated Event")))
                .andExpect(status().isOk());

        Event updated = eventRepository.findById(event.getId()).orElseThrow();
        assertEquals("Admin Updated Event", updated.getTitle());
    }

    private User saveUser(String email, String name, Role role) {
        User user = new User(email, name, null);
        user.setRole(role);
        return userRepository.save(user);
    }

    private Event event(String title, User owner) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription("Description");
        event.setType(EventType.EVENT);
        event.setCategory(EventCategory.TECH);
        event.setCity("Baku");
        event.setAddress("Nizami street");
        event.setStartDateTime(LocalDateTime.now().plusDays(1));
        event.setEndDateTime(LocalDateTime.now().plusDays(1).plusHours(2));
        event.setIsFree(false);
        event.setPrice(BigDecimal.TEN);
        event.setOrganizerName("Loopin");
        event.setStatus(EventStatus.PUBLISHED);
        event.setOwner(owner);
        return event;
    }

    private String eventPayload(String title) {
        return """
                {
                  "title": "%s",
                  "description": "Description",
                  "type": "EVENT",
                  "category": "TECH",
                  "city": "Baku",
                  "address": "Nizami street",
                  "startDateTime": "2030-01-01T10:00:00",
                  "endDateTime": "2030-01-01T12:00:00",
                  "isFree": false,
                  "price": 10.00,
                  "organizerName": "Loopin",
                  "status": "PUBLISHED"
                }
                """.formatted(title);
    }

    private String eventPayload(String title, Interest... interests) {
        String interestIds = java.util.Arrays.stream(interests)
                .map(interest -> "\"" + interest.getPublicId() + "\"")
                .collect(java.util.stream.Collectors.joining(", "));

        return """
                {
                  "title": "%s",
                  "description": "Description",
                  "type": "EVENT",
                  "category": "TECH",
                  "city": "Baku",
                  "address": "Nizami street",
                  "startDateTime": "2030-01-01T10:00:00",
                  "endDateTime": "2030-01-01T12:00:00",
                  "isFree": false,
                  "price": 10.00,
                  "organizerName": "Loopin",
                  "status": "PUBLISHED",
                  "interestIds": [%s]
                }
                """.formatted(title, interestIds);
    }

    @Test
    void getRecommendedEvents_ReturnsPublishedEventsAsFallback() throws Exception {
        Event event = eventRepository.save(event("Published Event", owner));
        Event draftEvent = event("Draft Event", owner);
        draftEvent.setStatus(EventStatus.DRAFT);
        eventRepository.save(draftEvent);
        Event deletedEvent = eventRepository.save(event("Deleted Event", owner));
        deletedEvent.markAsDeleted();
        eventRepository.save(deletedEvent);

        mockMvc.perform(post("/v1/events")
                        .header("Authorization", "Bearer " + otherUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventPayload("Another Event")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/events/recommended")
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", is("Another Event")))
                .andExpect(jsonPath("$[1].title", is("Published Event")));
    }

    private Interest interest(String name, String slug, String category) {
        Interest interest = new Interest();
        interest.setName(name);
        interest.setSlug(slug);
        interest.setCategory(category);
        return interest;
    }

    @Test
    void getPublishedEvents_ReturnsPaginatedEvents() throws Exception {
        eventRepository.save(event("Published Event 1", owner));
        eventRepository.save(event("Published Event 2", owner));

        mockMvc.perform(get("/v1/events")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].title", is("Published Event 1")))
                .andExpect(jsonPath("$.content[1].title", is("Published Event 2")))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.totalPages", is(1)));
    }

    @Test
    void getPublishedEvents_ReturnsInterestsWithoutNPlusOneQueries() {
        Interest tech = interestRepository.save(interest("Tech", "tech-list", "Professional"));
        Interest music = interestRepository.save(interest("Music", "music-list", "Culture"));
        Event first = eventRepository.save(event("Published Event 1", owner));
        Event second = eventRepository.save(event("Published Event 2", owner));
        Event third = eventRepository.save(event("Published Event 3", owner));
        assignInterest(first, tech);
        assignInterest(first, music);
        assignInterest(second, tech);
        assignInterest(third, music);
        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        var response = eventService.getPublishedEvents(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertEquals(3, response.getContent().size());
        assertEquals(2, response.getContent().get(0).getInterests().size());
        assertEquals("Music", response.getContent().get(0).getInterests().get(0).getName());
        assertEquals("Tech", response.getContent().get(0).getInterests().get(1).getName());
        assertTrue(
                statistics.getPrepareStatementCount() <= 3,
                "Expected fixed query count for page, count, and interest fetch, but executed "
                        + statistics.getPrepareStatementCount()
        );
    }

    @Test
    void getPublishedEventById_ReturnsInterests() throws Exception {
        Interest tech = interestRepository.save(interest("Tech", "tech-single", "Professional"));
        Event event = eventRepository.save(event("Published Event", owner));
        assignInterest(event, tech);
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/v1/events/" + event.getPublicId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Published Event")))
                .andExpect(jsonPath("$.interests", hasSize(1)))
                .andExpect(jsonPath("$.interests[0].name", is("Tech")));
    }

    @Test
    void getPublishedEventById_DoesNotReturnDraftOrDeletedEvents() throws Exception {
        Event draftEvent = event("Draft Event", owner);
        draftEvent.setStatus(EventStatus.DRAFT);
        draftEvent = eventRepository.save(draftEvent);
        Event deletedEvent = eventRepository.save(event("Deleted Event", owner));
        deletedEvent.markAsDeleted();
        eventRepository.save(deletedEvent);

        mockMvc.perform(get("/v1/events/" + draftEvent.getPublicId()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/v1/events/" + deletedEvent.getPublicId()))
                .andExpect(status().isNotFound());
    }

    private void assignInterest(Event event, Interest interest) {
        eventInterestRepository.save(new EventInterest(event, interest));
    }
}
