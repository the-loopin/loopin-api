package com.loopin.api.events.create;

import com.loopin.api.common.exception.DuplicateResourceException;
import com.loopin.api.events.dto.request.EventCreateRequest;
import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.enums.EventCategory;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.events.enums.EventType;
import com.loopin.api.events.mapper.EventMapper;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.events.shared.finder.EventFinder;
import com.loopin.api.events.shared.interest.EventInterestManager;
import com.loopin.api.events.shared.moderation.EventModerationManager;
import com.loopin.api.events.shared.validation.EventValidator;
import com.loopin.api.notifications.service.NotificationService;
import com.loopin.api.recommendation.event.EventEmbeddingService;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateEventHandlerTest {

    private EventRepository eventRepository;
    private EventMapper eventMapper;
    private EventFinder eventFinder;
    private EventValidator eventValidator;
    private EventInterestManager eventInterestManager;
    private EventModerationManager eventModerationManager;
    private EventEmbeddingService eventEmbeddingService;
    private NotificationService notificationService;
    private CreateEventHandler handler;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        eventMapper = mock(EventMapper.class);
        eventFinder = mock(EventFinder.class);
        eventValidator = mock(EventValidator.class);
        eventInterestManager = mock(EventInterestManager.class);
        eventModerationManager = mock(EventModerationManager.class);
        eventEmbeddingService = mock(EventEmbeddingService.class);
        notificationService = mock(NotificationService.class);
        handler = new CreateEventHandler(
                eventRepository, eventMapper, eventFinder, eventValidator, eventInterestManager,
                eventModerationManager, eventEmbeddingService, notificationService
        );
    }

    @Test
    void handle_ValidCommand_PersistsOwnedEventAndTriggersSideEffects() {
        EventCreateRequest request = validRequest();
        User owner = new User("owner@example.test", "Owner", null);
        Event event = new Event();
        event.setTitle(request.getTitle());
        event.setPublicId(UUID.randomUUID());
        EventResponse response = mock(EventResponse.class);
        when(eventFinder.findCurrentUser("owner@example.test")).thenReturn(owner);
        when(eventMapper.toEntity(request)).thenReturn(event);
        when(eventRepository.saveAndFlush(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(response);

        EventResponse result = handler.handle(new CreateEventCommand(request, "owner@example.test"));

        assertEquals(response, result);
        assertEquals(owner, event.getOwner());
        assertEquals(EventStatus.PUBLISHED, event.getStatus());
        verify(eventValidator).validateDateRange(request.getStartDateTime(), request.getEndDateTime());
        verify(eventValidator).validatePrice(request.getIsFree(), request.getPrice());
        verify(eventValidator).validateNoDuplicate(request.getTitle(), request.getCity(), request.getStartDateTime());
        verify(eventModerationManager).apply(event, request.getTitle(), request.getDescription());
        verify(eventInterestManager).replace(event, request.getInterestIds());
        verify(eventEmbeddingService).indexEvent(event);
        verify(notificationService).create(any());
    }

    @Test
    void handle_DuplicateEvent_StopsBeforePersistence() {
        EventCreateRequest request = validRequest();
        when(eventFinder.findCurrentUser("owner@example.test")).thenReturn(new User());
        doThrow(new DuplicateResourceException("duplicate"))
                .when(eventValidator).validateNoDuplicate(request.getTitle(), request.getCity(), request.getStartDateTime());

        assertThrows(DuplicateResourceException.class,
                () -> handler.handle(new CreateEventCommand(request, "owner@example.test")));

        verify(eventRepository, never()).saveAndFlush(any());
        verify(eventEmbeddingService, never()).indexEvent(any());
        verify(notificationService, never()).create(any());
    }

    @Test
    void handle_InvalidDateRange_StopsBeforePersistence() {
        EventCreateRequest request = validRequest();
        when(eventFinder.findCurrentUser("owner@example.test")).thenReturn(new User());
        doThrow(new IllegalArgumentException("End date and time must be after start date and time"))
                .when(eventValidator).validateDateRange(request.getStartDateTime(), request.getEndDateTime());

        assertThrows(IllegalArgumentException.class,
                () -> handler.handle(new CreateEventCommand(request, "owner@example.test")));

        verify(eventRepository, never()).saveAndFlush(any());
    }

    @Test
    void handle_InvalidPrice_StopsBeforePersistence() {
        EventCreateRequest request = validRequest();
        when(eventFinder.findCurrentUser("owner@example.test")).thenReturn(new User());
        doThrow(new IllegalArgumentException("Paid events must have price greater than 0"))
                .when(eventValidator).validatePrice(request.getIsFree(), request.getPrice());

        assertThrows(IllegalArgumentException.class,
                () -> handler.handle(new CreateEventCommand(request, "owner@example.test")));

        verify(eventRepository, never()).saveAndFlush(any());
    }

    @Test
    void handle_InvalidInterestIdentifiers_DoesNotTriggerExternalSideEffects() {
        EventCreateRequest request = validRequest();
        Event event = new Event();
        when(eventFinder.findCurrentUser("owner@example.test")).thenReturn(new User());
        when(eventMapper.toEntity(request)).thenReturn(event);
        when(eventRepository.saveAndFlush(event)).thenReturn(event);
        doThrow(new IllegalArgumentException("Duplicate interests are not allowed."))
                .when(eventInterestManager).replace(event, request.getInterestIds());

        assertThrows(IllegalArgumentException.class,
                () -> handler.handle(new CreateEventCommand(request, "owner@example.test")));

        verify(eventEmbeddingService, never()).indexEvent(any());
        verify(notificationService, never()).create(any());
    }

    private EventCreateRequest validRequest() {
        EventCreateRequest request = new EventCreateRequest();
        request.setTitle("Event title");
        request.setDescription("Event description");
        request.setType(EventType.EVENT);
        request.setCategory(EventCategory.TECH);
        request.setCity("Baku");
        request.setStartDateTime(LocalDateTime.of(2030, 1, 1, 10, 0));
        request.setEndDateTime(LocalDateTime.of(2030, 1, 1, 12, 0));
        request.setIsFree(true);
        request.setPrice(BigDecimal.ZERO);
        request.setOrganizerName("Loopin");
        return request;
    }
}
