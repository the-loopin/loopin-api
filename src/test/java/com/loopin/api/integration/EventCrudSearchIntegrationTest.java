package com.loopin.api.integration;

import com.jayway.jsonpath.JsonPath;
import com.loopin.api.auth.dto.GoogleLoginRequest;
import com.loopin.api.auth.enums.Role;
import com.loopin.api.auth.service.GoogleTokenClaims;
import com.loopin.api.auth.service.GoogleTokenVerifier;
import com.loopin.api.common.enums.EventCategory;
import com.loopin.api.common.enums.EventStatus;
import com.loopin.api.common.enums.EventType;
import com.loopin.api.dto.event.request.EventCreateRequest;
import com.loopin.api.dto.event.request.EventUpdateRequest;
import com.loopin.api.entity.User;
import com.loopin.api.entity.UserProfile;
import com.loopin.api.repository.EventRepository;
import com.loopin.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventCrudSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GoogleTokenVerifier googleTokenVerifier;

    private String userToken;
    private User testUser;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        GoogleTokenVerifier googleTokenVerifier() {
            return mock(GoogleTokenVerifier.class);
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        reset(googleTokenVerifier);
        eventRepository.deleteAll();
        userRepository.deleteAll();

        // Seed a regular USER
        testUser = new User("user@email.com", "Test User", "user-google-id");
        testUser.setRole(Role.USER);
        UserProfile userProfile = new UserProfile();
        userProfile.setUser(testUser);
        userProfile.setName("Test User");
        testUser.setProfile(userProfile);
        userRepository.save(testUser);

        when(googleTokenVerifier.verify("valid-user-token"))
                .thenReturn(new GoogleTokenClaims("user-google-id", "user@email.com", "Test User"));

        GoogleLoginRequest userRequest = new GoogleLoginRequest("valid-user-token");
        MvcResult userLoginResult = mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isOk())
                .andReturn();
        userToken = JsonPath.read(userLoginResult.getResponse().getContentAsString(), "$.token");
    }

    @Test
    void createEvent_ValidRequest_ReturnsCreatedAndSavesToDb() throws Exception {
        EventCreateRequest createRequest = new EventCreateRequest();
        createRequest.setTitle("New API Event");
        createRequest.setDescription("Event Description");
        createRequest.setType(EventType.EVENT);
        createRequest.setCategory(EventCategory.TECH);
        createRequest.setCity("Dubai");
        createRequest.setAddress("Downtown");
        createRequest.setStartDateTime(LocalDateTime.now().plusDays(1).withNano(0));
        createRequest.setEndDateTime(LocalDateTime.now().plusDays(2).withNano(0));
        createRequest.setIsFree(true);
        createRequest.setPrice(BigDecimal.ZERO);
        createRequest.setOrganizerName("Tech Hub");
        createRequest.setStatus(EventStatus.PUBLISHED);

        long initialCount = eventRepository.count();

        mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("New API Event")))
                .andExpect(jsonPath("$.type", is("EVENT")))
                .andExpect(jsonPath("$.category", is("TECH")))
                .andExpect(jsonPath("$.city", is("Dubai")));

        assertEquals(initialCount + 1, eventRepository.count());
    }

    @Test
    void createEvent_InvalidRequestMissingTitle_ReturnsBadRequest() throws Exception {
        EventCreateRequest createRequest = new EventCreateRequest();
        // Missing title
        createRequest.setDescription("Event Description");
        createRequest.setType(EventType.EVENT);
        createRequest.setCategory(EventCategory.TECH);
        createRequest.setCity("Dubai");
        createRequest.setStartDateTime(LocalDateTime.now().plusDays(1));
        createRequest.setEndDateTime(LocalDateTime.now().plusDays(2));
        createRequest.setIsFree(true);
        createRequest.setPrice(BigDecimal.ZERO);
        createRequest.setOrganizerName("Tech Hub");
        createRequest.setStatus(EventStatus.PUBLISHED);

        mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors.title", notNullValue()));
    }

    @Test
    void createEvent_UnauthenticatedRequest_ReturnsUnauthorized() throws Exception {
        EventCreateRequest createRequest = new EventCreateRequest();
        createRequest.setTitle("Unauthorized Event");
        // ... (other fields don't matter as it should fail on auth first)

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getEventById_ExistingEvent_ReturnsOk() throws Exception {
        String eventId = createTestEvent("Test Event", EventCategory.TECH, "Dubai", EventType.EVENT);

        mockMvc.perform(get("/events/" + eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(eventId)))
                .andExpect(jsonPath("$.title", is("Test Event")));
    }

    @Test
    void getEventById_NonexistentId_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/events/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void updateEvent_ValidOwner_ReturnsOkAndPersistsChanges() throws Exception {
        String eventId = createTestEvent("Original Title", EventCategory.TECH, "Dubai", EventType.EVENT);

        EventUpdateRequest updateRequest = new EventUpdateRequest();
        updateRequest.setTitle("Updated Title");
        updateRequest.setDescription("Updated Description");
        updateRequest.setType(EventType.EVENT);
        updateRequest.setCategory(EventCategory.TECH);
        updateRequest.setCity("Abu Dhabi");
        updateRequest.setAddress("Corniche");
        updateRequest.setStartDateTime(LocalDateTime.now().plusDays(3).withNano(0));
        updateRequest.setEndDateTime(LocalDateTime.now().plusDays(4).withNano(0));
        updateRequest.setIsFree(false);
        updateRequest.setPrice(new BigDecimal("100.00"));
        updateRequest.setOrganizerName("Updated Hub");
        updateRequest.setStatus(EventStatus.PUBLISHED);

        mockMvc.perform(put("/events/" + eventId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Title")))
                .andExpect(jsonPath("$.city", is("Abu Dhabi")))
                .andExpect(jsonPath("$.price", is(100.0)));

        // Verify via a follow-up GET
        mockMvc.perform(get("/events/" + eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Title")));
    }

    @Test
    void deleteEvent_ValidOwner_SoftDeletesAndSubsequentGetReturnsNotFound() throws Exception {
        String eventId = createTestEvent("To Be Deleted", EventCategory.TECH, "Dubai", EventType.EVENT);

        // Delete the event
        mockMvc.perform(delete("/events/" + eventId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        // Since it's soft-deleted, getPublishedEventById excludes notDeleted(), meaning it throws NoSuchElementException -> 404
        mockMvc.perform(get("/events/" + eventId))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchEvents_MultipleFilters_ReturnsOnlyMatchingEvents() throws Exception {
        createTestEvent("Tech Meetup Dubai", EventCategory.TECH, "Dubai", EventType.EVENT);
        createTestEvent("Startup Pitch Dubai", EventCategory.STARTUP, "Dubai", EventType.EVENT);
        createTestEvent("Tech Meetup London", EventCategory.TECH, "London", EventType.EVENT);
        createTestEvent("Tech Activity Dubai", EventCategory.TECH, "Dubai", EventType.ACTIVITY);

        // Filter by category=TECH and city=Dubai
        mockMvc.perform(get("/events?category=TECH&city=Dubai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2))) // "Tech Meetup Dubai" and "Tech Activity Dubai"
                .andExpect(jsonPath("$[*].title", containsInAnyOrder("Tech Meetup Dubai", "Tech Activity Dubai")));

        // Filter by type=EVENT, category=TECH, city=Dubai
        mockMvc.perform(get("/events?type=EVENT&category=TECH&city=Dubai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].title", is("Tech Meetup Dubai")));

        // Filter by search string "startup"
        mockMvc.perform(get("/events?search=startup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].title", is("Startup Pitch Dubai")));
    }

    @Test
    void searchEvents_NoResultsMatch_ReturnsEmptyList() throws Exception {
        createTestEvent("Tech Meetup", EventCategory.TECH, "Dubai", EventType.EVENT);

        mockMvc.perform(get("/events?city=Tokyo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    /**
     * Helper to create events via API, as instructed: "create events THROUGH the API (POST /events) within each test method"
     */
    private String createTestEvent(String title, EventCategory category, String city, EventType type) throws Exception {
        EventCreateRequest createRequest = new EventCreateRequest();
        createRequest.setTitle(title);
        createRequest.setDescription("Description for " + title);
        createRequest.setType(type);
        createRequest.setCategory(category);
        createRequest.setCity(city);
        createRequest.setStartDateTime(LocalDateTime.now().plusDays(1).withNano(0));
        createRequest.setEndDateTime(LocalDateTime.now().plusDays(2).withNano(0));
        createRequest.setIsFree(true);
        createRequest.setPrice(BigDecimal.ZERO);
        createRequest.setOrganizerName("Organizer");
        createRequest.setStatus(EventStatus.PUBLISHED);

        MvcResult result = mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }
}
