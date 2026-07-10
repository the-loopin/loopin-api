package com.loopin.api.events.service;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.events.enums.EventCategory;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.events.enums.EventType;
import com.loopin.api.groups.enums.GroupStatus;
import com.loopin.api.common.exception.DuplicateResourceException;
import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.events.dto.request.EventCreateRequest;
import com.loopin.api.events.dto.request.EventUpdateRequest;
import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.entity.EventGroup;
import com.loopin.api.users.entity.User;
import com.loopin.api.events.mapper.EventMapper;
import com.loopin.api.recommendation.event.EventEmbeddingService;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.events.repository.EventGroupRepository;
import com.loopin.api.users.repository.UserRepository;

import com.loopin.api.interests.repository.InterestRepository;
import com.loopin.api.events.repository.EventInterestRepository;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.notifications.service.NotificationService;
import com.loopin.api.recommendation.user.UserEmbeddingRepository;
import com.loopin.api.recommendation.event.EventEmbeddingRepository;
import com.loopin.api.moderation.ContentModerationProperties;
import com.loopin.api.moderation.ContentModerationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.ArrayList;
import com.loopin.api.interests.dto.InterestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
    private EventGroupRepository eventGroupRepository;
    private EventEmbeddingService eventEmbeddingService;
    private InterestRepository interestRepository;
    private EventInterestRepository eventInterestRepository;
    private UserEmbeddingRepository userEmbeddingRepository;
    private EventEmbeddingRepository eventEmbeddingRepository;
    private GroupMemberRepository groupMemberRepository;
    private NotificationService notificationService;


    private EventServiceImpl eventService;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        eventMapper = mock(EventMapper.class);
        userRepository = mock(UserRepository.class);
        eventGroupRepository = mock(EventGroupRepository.class);
        eventEmbeddingService = mock(EventEmbeddingService.class);
        interestRepository = mock(InterestRepository.class);
        eventInterestRepository = mock(EventInterestRepository.class);
        userEmbeddingRepository = mock(UserEmbeddingRepository.class);
        eventEmbeddingRepository = mock(EventEmbeddingRepository.class);
        groupMemberRepository = mock(GroupMemberRepository.class);
        notificationService = mock(NotificationService.class);


        eventService = new EventServiceImpl(
                eventRepository,
                eventMapper,
                userRepository,
                eventGroupRepository,
                interestRepository,
                eventInterestRepository,
                eventEmbeddingService,
                userEmbeddingRepository,
                eventEmbeddingRepository,
                groupMemberRepository,
                notificationService,
                new ContentModerationService(new ContentModerationProperties())
        );
    }

    @Test
    void getPublishedEvents_Valid_ReturnsListOfEvents() {
        Event event = event(1L, EVENT_ID);
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(event)));
        when(eventRepository.findPublishedByIdInWithInterests(any())).thenReturn(List.of(event));
        
        EventResponse realResponse = eventResponse(EVENT_ID);
        when(eventMapper.toResponse(any())).thenReturn(realResponse);

        List<EventResponse> result = eventService.getPublishedEvents(
                EventType.EVENT, EventCategory.TECH, "City", true, "Search", LocalDate.now(), LocalDate.now().plusDays(1), Pageable.unpaged()
        ).getContent();

        assertEquals(1, result.size());
        assertEquals(realResponse, result.get(0));
        assertEquals(realResponse.getTitle(), result.get(0).getTitle());
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
        when(eventMapper.toEntity(any(EventCreateRequest.class))).thenReturn(mappedEvent);
        when(eventRepository.saveAndFlush(mappedEvent)).thenReturn(mappedEvent);

        EventResponse realResponse = eventResponse(EVENT_ID);
        when(eventMapper.toResponse(mappedEvent)).thenReturn(realResponse);

        EventResponse result = eventService.createEvent(request, USERNAME);

        assertEquals(user, mappedEvent.getOwner());
        verify(eventRepository).saveAndFlush(mappedEvent);
        verify(eventEmbeddingService).indexEvent(mappedEvent);
        verify(notificationService).create(any());
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
    void createEvent_BlockedContent_IsSavedAsDraft() {
        User user = user(1L, USER_ID, Role.USER);
        when(userRepository.findByEmailAndDeletedAtIsNull(USERNAME)).thenReturn(Optional.of(user));
        when(eventRepository.exists(any(Specification.class))).thenReturn(false);

        ContentModerationProperties properties = new ContentModerationProperties();
        properties.setBannedWords(List.of("scam"));
        eventService = new EventServiceImpl(
                eventRepository, eventMapper, userRepository, eventGroupRepository, interestRepository,
                eventInterestRepository, eventEmbeddingService, userEmbeddingRepository, eventEmbeddingRepository,
                groupMemberRepository, notificationService, new ContentModerationService(properties));

        EventCreateRequest request = new EventCreateRequest();
        request.setStartDateTime(LocalDateTime.now().plusDays(1));
        request.setEndDateTime(LocalDateTime.now().plusDays(2));
        request.setIsFree(true);
        request.setPrice(BigDecimal.ZERO);
        request.setTitle("Not a SCAM");
        request.setDescription("Join us");
        request.setCity("Test City");

        Event mappedEvent = new Event();
        when(eventMapper.toEntity(request)).thenReturn(mappedEvent);
        when(eventRepository.saveAndFlush(mappedEvent)).thenReturn(mappedEvent);
        when(eventMapper.toResponse(mappedEvent)).thenReturn(eventResponse(EVENT_ID));

        eventService.createEvent(request, USERNAME);

        assertEquals(EventStatus.DRAFT, mappedEvent.getStatus());
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
        when(eventMapper.toResponse(any())).thenReturn(realResponse);

        EventResponse result = eventService.updateEvent(EVENT_ID, request, USERNAME);

        verify(eventMapper).updateEntity(event, request);
        verify(eventRepository).save(event);
        verify(eventEmbeddingService).indexEvent(event);
        verify(notificationService).createAll(any());
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

        assertThrows(com.loopin.api.common.exception.ForbiddenAccessException.class, () -> eventService.updateEvent(EVENT_ID, request, USERNAME));
    }

    @Test
    void deleteEvent_ValidOwner_MarksAsDeleted() {
        User user = user(1L, USER_ID, Role.USER);
        when(userRepository.findByEmailAndDeletedAtIsNull(USERNAME)).thenReturn(Optional.of(user));

        Event event = event(10L, EVENT_ID);
        EventGroup group = new EventGroup();
        group.setStatus(GroupStatus.OPEN);

        event.setOwner(user);
        when(eventRepository.findOne(any(Specification.class))).thenReturn(Optional.of(event));
        when(eventGroupRepository.findByEventIdAndStatusNot(event.getId(), GroupStatus.ARCHIVED))
                .thenReturn(List.of(group));

        eventService.deleteEvent(EVENT_ID, USERNAME);

        assertEquals(GroupStatus.ARCHIVED, group.getStatus());
        verify(eventGroupRepository).save(group);
        verify(eventRepository).save(event);
        verify(notificationService).createAll(any());
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
                new BigDecimal("40.376200"), new BigDecimal("49.844700"),
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), true, BigDecimal.ZERO,
                "Organizer", "Image", EventStatus.PUBLISHED, new ArrayList<>(), LocalDateTime.now(), LocalDateTime.now()
        );
    }
}
