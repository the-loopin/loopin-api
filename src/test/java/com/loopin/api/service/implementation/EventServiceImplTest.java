package com.loopin.api.service.implementation;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.common.enums.EventCategory;
import com.loopin.api.common.enums.EventStatus;
import com.loopin.api.common.enums.EventType;
import com.loopin.api.common.exception.DuplicateResourceException;
import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.dto.event.request.EventCreateRequest;
import com.loopin.api.dto.event.request.EventUpdateRequest;
import com.loopin.api.dto.event.response.EventResponse;
import com.loopin.api.entity.Event;
import com.loopin.api.entity.User;
import com.loopin.api.mapper.EventMapper;
import com.loopin.api.recommendation.EventEmbeddingService;
import com.loopin.api.repository.EventRepository;
import com.loopin.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventServiceImplTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String USERNAME = "user@test.com";

    private EventRepository eventRepository;
    private EventMapper eventMapper;
    private UserRepository userRepository;
    private EventEmbeddingService eventEmbeddingService;

    private EventServiceImpl eventService;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        eventMapper = mock(EventMapper.class);
        userRepository = mock(UserRepository.class);
        eventEmbeddingService = mock(EventEmbeddingService.class);

        eventService = new EventServiceImpl(
                eventRepository,
                eventMapper,
                userRepository,
                eventEmbeddingService
        );
    }

    @Test
    void getPublishedEvents_Valid_ReturnsListOfEvents() {
        Event event = event(1L, EVENT_ID);
        when(eventRepository.findAll(any(Specification.class))).thenReturn(List.of(event));
        
        EventResponse realResponse = eventResponse(EVENT_ID);
        when(eventMapper.toResponse(event)).thenReturn(realResponse);

        List<EventResponse> result = eventService.getPublishedEvents(
                EventType.EVENT, EventCategory.TECH, "City", true, "Search", LocalDate.now(), LocalDate.now().plusDays(1)
        );

        assertEquals(1, result.size());
        assertEquals(realResponse, result.get(0));
        assertEquals(realResponse.getTitle(), result.get(0).getTitle());
    }

    @Test
    void getPublishedEventById_Found_ReturnsEvent() {
        Event event = event(1L, EVENT_ID);
        when(eventRepository.findOne(any(Specification.class))).thenReturn(Optional.of(event));
        
        EventResponse realResponse = eventResponse(EVENT_ID);
        when(eventMapper.toResponse(event)).thenReturn(realResponse);

        EventResponse result = eventService.getPublishedEventById(EVENT_ID);

        assertEquals(realResponse, result);
        assertEquals(realResponse.getTitle(), result.getTitle());
    }

    @Test
    void getPublishedEventById_NotFound_ThrowsNoSuchElementException() {
        when(eventRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> eventService.getPublishedEventById(EVENT_ID));
    }

    @Test
    void createEvent_ValidRequest_CreatesAndIndexesEvent() {
        User user = user(1L, USER_ID, Role.USER);
        when(userRepository.findByEmailAndDeletedAtIsNull(USERNAME)).thenReturn(Optional.of(user));

        EventCreateRequest request = new EventCreateRequest();
        request.setStartDateTime(LocalDateTime.now().plusDays(1));
        request.setEndDateTime(LocalDateTime.now().plusDays(2));
        request.setIsFree(true);
        request.setPrice(BigDecimal.ZERO);
        request.setTitle("Test Event");
        request.setCity("Test City");

        when(eventRepository.exists(any(Specification.class))).thenReturn(false);

        Event mappedEvent = new Event();
        when(eventMapper.toEntity(request)).thenReturn(mappedEvent);
        when(eventRepository.save(mappedEvent)).thenReturn(mappedEvent);

        EventResponse realResponse = eventResponse(EVENT_ID);
        when(eventMapper.toResponse(mappedEvent)).thenReturn(realResponse);

        EventResponse result = eventService.createEvent(request, USERNAME);

        assertEquals(user, mappedEvent.getOwner());
        verify(eventRepository).save(mappedEvent);
        verify(eventEmbeddingService).indexEvent(mappedEvent);
        assertEquals(realResponse, result);
        assertEquals(realResponse.getTitle(), result.getTitle());
    }

    @Test
    void createEvent_DuplicateExists_ThrowsDuplicateResourceException() {
        User user = user(1L, USER_ID, Role.USER);
        when(userRepository.findByEmailAndDeletedAtIsNull(USERNAME)).thenReturn(Optional.of(user));

        EventCreateRequest request = new EventCreateRequest();
        request.setStartDateTime(LocalDateTime.now().plusDays(1));
        request.setEndDateTime(LocalDateTime.now().plusDays(2));
        request.setIsFree(true);
        request.setPrice(BigDecimal.ZERO);
        request.setTitle("Test Event");
        request.setCity("Test City");

        when(eventRepository.exists(any(Specification.class))).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> eventService.createEvent(request, USERNAME));
    }

    @Test
    void updateEvent_ValidOwner_UpdatesAndIndexesEvent() {
        User user = user(1L, USER_ID, Role.USER);
        when(userRepository.findByEmailAndDeletedAtIsNull(USERNAME)).thenReturn(Optional.of(user));

        Event event = event(10L, EVENT_ID);
        event.setOwner(user);
        when(eventRepository.findOne(any(Specification.class))).thenReturn(Optional.of(event));

        EventUpdateRequest request = new EventUpdateRequest();
        request.setStartDateTime(LocalDateTime.now().plusDays(1));
        request.setEndDateTime(LocalDateTime.now().plusDays(2));
        request.setIsFree(true);
        request.setPrice(BigDecimal.ZERO);

        when(eventRepository.save(event)).thenReturn(event);
        EventResponse realResponse = eventResponse(EVENT_ID);
        when(eventMapper.toResponse(event)).thenReturn(realResponse);

        EventResponse result = eventService.updateEvent(EVENT_ID, request, USERNAME);

        verify(eventMapper).updateEntity(event, request);
        verify(eventRepository).save(event);
        verify(eventEmbeddingService).indexEvent(event);
        assertEquals(realResponse, result);
        assertEquals(realResponse.getTitle(), result.getTitle());
    }

    @Test
    void updateEvent_NotOwner_ThrowsForbidden() {
        User user = user(1L, USER_ID, Role.USER);
        User owner = user(2L, UUID.randomUUID(), Role.USER);
        when(userRepository.findByEmailAndDeletedAtIsNull(USERNAME)).thenReturn(Optional.of(user));

        Event event = event(10L, EVENT_ID);
        event.setOwner(owner);
        when(eventRepository.findOne(any(Specification.class))).thenReturn(Optional.of(event));

        EventUpdateRequest request = new EventUpdateRequest();
        request.setStartDateTime(LocalDateTime.now().plusDays(1));
        request.setEndDateTime(LocalDateTime.now().plusDays(2));
        request.setIsFree(true);
        request.setPrice(BigDecimal.ZERO);

        assertThrows(ResponseStatusException.class, () -> eventService.updateEvent(EVENT_ID, request, USERNAME));
    }

    @Test
    void deleteEvent_ValidOwner_MarksAsDeleted() {
        User user = user(1L, USER_ID, Role.USER);
        when(userRepository.findByEmailAndDeletedAtIsNull(USERNAME)).thenReturn(Optional.of(user));

        Event event = event(10L, EVENT_ID);
        event.setOwner(user);
        when(eventRepository.findOne(any(Specification.class))).thenReturn(Optional.of(event));

        eventService.deleteEvent(EVENT_ID, USERNAME);

        verify(eventRepository).save(event);
        // The event object's internal state for deletedAt should be updated via markAsDeleted()
        // Here we just verify the save call on the modified object
    }

    private User user(Long id, UUID publicId, Role role) {
        User user = new User(USERNAME, "Test User", null);
        user.setId(id);
        user.setPublicId(publicId);
        user.setRole(role);
        return user;
    }

    private Event event(Long id, UUID publicId) {
        Event event = new Event();
        event.setId(id);
        event.setPublicId(publicId);
        event.setStatus(EventStatus.PUBLISHED);
        return event;
    }

    private EventResponse eventResponse(UUID id) {
        return new EventResponse(
                id, "Title", "Desc", EventType.EVENT, EventCategory.TECH, "City", "Address",
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), true, BigDecimal.ZERO,
                "Organizer", "Image", EventStatus.PUBLISHED, LocalDateTime.now(), LocalDateTime.now()
        );
    }
}
