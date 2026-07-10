package com.loopin.api.events.service;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.groups.enums.GroupStatus;
import com.loopin.api.events.dto.request.EventUpdateRequest;
import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.entity.EventGroup;
import com.loopin.api.users.entity.User;
import com.loopin.api.events.mapper.EventMapper;
import com.loopin.api.recommendation.event.EventEmbeddingService;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.events.repository.EventGroupRepository;
import com.loopin.api.events.repository.EventInterestRepository;
import com.loopin.api.events.shared.finder.EventFinder;
import com.loopin.api.events.shared.interest.EventInterestManager;
import com.loopin.api.events.shared.moderation.EventModerationManager;
import com.loopin.api.events.shared.validation.EventValidator;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.notifications.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private EventGroupRepository eventGroupRepository;
    private EventEmbeddingService eventEmbeddingService;
    private EventInterestRepository eventInterestRepository;
    private GroupMemberRepository groupMemberRepository;
    private NotificationService notificationService;
    private EventFinder eventFinder;
    private EventValidator eventValidator;
    private EventInterestManager eventInterestManager;
    private EventModerationManager eventModerationManager;


    private EventServiceImpl eventService;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        eventMapper = mock(EventMapper.class);
        eventGroupRepository = mock(EventGroupRepository.class);
        eventEmbeddingService = mock(EventEmbeddingService.class);
        eventInterestRepository = mock(EventInterestRepository.class);
        groupMemberRepository = mock(GroupMemberRepository.class);
        notificationService = mock(NotificationService.class);
        eventFinder = mock(EventFinder.class);
        eventValidator = mock(EventValidator.class);
        eventInterestManager = mock(EventInterestManager.class);
        eventModerationManager = mock(EventModerationManager.class);


        eventService = new EventServiceImpl(
                eventRepository,
                eventMapper,
                eventGroupRepository,
                eventInterestRepository,
                eventEmbeddingService,
                groupMemberRepository,
                notificationService,
                eventFinder,
                eventValidator,
                eventInterestManager,
                eventModerationManager
        );
    }

    @Test
    void updateEvent_ValidOwner_UpdatesAndIndexesEvent() {
        User user = user(1L, USER_ID, Role.USER);
        when(eventFinder.findCurrentUser(USERNAME)).thenReturn(user);

        Event event = event(10L, EVENT_ID);
        event.setOwner(user);
        when(eventFinder.findActiveEventById(EVENT_ID)).thenReturn(event);

        EventUpdateRequest request = new EventUpdateRequest();
        request.setStartDateTime(LocalDateTime.now().plusDays(1));
        request.setEndDateTime(LocalDateTime.now().plusDays(2));
        request.setIsFree(true);
        request.setPrice(BigDecimal.ZERO);

        when(eventRepository.save(event)).thenReturn(event);
        EventResponse realResponse = mock(EventResponse.class);
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
        when(eventFinder.findCurrentUser(USERNAME)).thenReturn(user);

        Event event = event(10L, EVENT_ID);
        event.setOwner(owner);
        when(eventFinder.findActiveEventById(EVENT_ID)).thenReturn(event);

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
        when(eventFinder.findCurrentUser(USERNAME)).thenReturn(user);

        Event event = event(10L, EVENT_ID);
        EventGroup group = new EventGroup();
        group.setStatus(GroupStatus.OPEN);

        event.setOwner(user);
        when(eventFinder.findActiveEventById(EVENT_ID)).thenReturn(event);
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

}
