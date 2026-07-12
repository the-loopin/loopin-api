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
import com.loopin.api.events.shared.validation.EventRequestValidator;
import com.loopin.api.events.shared.validation.EventRequestValidationException;
import com.loopin.api.notifications.api.NotificationWriter;
import com.loopin.api.recommendation.api.RecommendationIndexer;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Map;

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
    private EventRequestValidator eventRequestValidator;
    private EventInterestManager eventInterestManager;
    private EventModerationManager eventModerationManager;
    private RecommendationIndexer recommendationIndexer;
    private NotificationWriter notificationWriter;
    private CreateEventHandler handler;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        eventMapper = mock(EventMapper.class);
        eventFinder = mock(EventFinder.class);
        eventValidator = mock(EventValidator.class);
        eventRequestValidator = mock(EventRequestValidator.class);
        eventInterestManager = mock(EventInterestManager.class);
        eventModerationManager = mock(EventModerationManager.class);
        recommendationIndexer = mock(RecommendationIndexer.class);
        notificationWriter = mock(NotificationWriter.class);
        handler = new CreateEventHandler(
                eventRepository, eventMapper, eventFinder, eventValidator, eventRequestValidator, eventInterestManager,
                eventModerationManager, recommendationIndexer, notificationWriter
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
        verify(eventRequestValidator).validate(request);
        verify(eventValidator).validateNoDuplicate(request.getTitle(), request.getCity(), request.getStartDateTime());
        verify(eventModerationManager).apply(event, request.getTitle(), request.getDescription());
        verify(eventInterestManager).replace(event, request.getInterestIds());
        verify(recommendationIndexer).index(event);
        verify(notificationWriter).write(any());
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
        verify(recommendationIndexer, never()).index(any());
        verify(notificationWriter, never()).write(any());
    }

    @Test
    void handle_InvalidRequest_StopsBeforePersistence() {
        EventCreateRequest request = validRequest();
        doThrow(new EventRequestValidationException(Map.of("endDateTime", "invalid")))
                .when(eventRequestValidator).validate(request);

        assertThrows(EventRequestValidationException.class,
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

        verify(recommendationIndexer, never()).index(any());
        verify(notificationWriter, never()).write(any());
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
