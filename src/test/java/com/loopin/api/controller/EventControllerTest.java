package com.loopin.api.controller;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.common.enums.EventCategory;
import com.loopin.api.common.enums.EventStatus;
import com.loopin.api.common.enums.EventType;
import com.loopin.api.common.security.JwtUtils;
import com.loopin.api.entity.Event;
import com.loopin.api.entity.Interest;
import com.loopin.api.entity.User;
import com.loopin.api.repository.EventInterestRepository;
import com.loopin.api.repository.EventRepository;
import com.loopin.api.repository.InterestRepository;
import com.loopin.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@SpringBootTest
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
    private JwtUtils jwtUtils;

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
        mockMvc.perform(post("/events")
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

        mockMvc.perform(post("/events")
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

        mockMvc.perform(put("/events/" + event.getPublicId())
                        .header("Authorization", "Bearer " + otherUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventPayload("Hijacked Event")))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateEvent_AdminCanUpdateAnyEvent() throws Exception {
        Event event = eventRepository.save(event("Owned Event", owner));

        mockMvc.perform(put("/events/" + event.getPublicId())
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

        mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + otherUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventPayload("Another Event")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/events/recommended")
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
}
