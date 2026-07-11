package com.loopin.api.events.loopinevent;

import com.loopin.api.common.exception.InvalidEventStateException;
import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.dto.response.LoopedEventResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.events.mapper.EventMapper;
import com.loopin.api.events.repository.UserLoopedEventRepository;
import com.loopin.api.events.shared.finder.EventFinder;
import com.loopin.api.events.shared.policy.EventLoopInPolicy;
import com.loopin.api.moderation.enums.ContentModerationStatus;
import com.loopin.api.notifications.api.NotificationWriter;
import com.loopin.api.notifications.enums.NotificationReferenceType;
import com.loopin.api.notifications.enums.NotificationType;
import com.loopin.api.notifications.service.NotificationCommand;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LoopInEventHandlerTest {

    private static final String USER_EMAIL = "participant@example.test";

    private EventFinder eventFinder;
    private EventLoopInPolicy eventLoopInPolicy;
    private UserLoopedEventRepository loopedEventRepository;
    private EventMapper eventMapper;
    private NotificationWriter notificationWriter;

    private LoopInEventHandler handler;

    @BeforeEach
    void setUp() {
        eventFinder = mock(EventFinder.class);
        eventLoopInPolicy = mock(EventLoopInPolicy.class);
        loopedEventRepository = mock(UserLoopedEventRepository.class);
        eventMapper = mock(EventMapper.class);
        notificationWriter = mock(NotificationWriter.class);

        handler = new LoopInEventHandler(
            eventFinder,
            eventLoopInPolicy,
            loopedEventRepository,
            eventMapper,
            notificationWriter
        );
    }

    @Test
    void handle_NewLoopIn_CreatesRelationNotifiesOwnerAndReturnsCount() {
        User participant = user(1L, "Participant");
        User owner = user(2L, "Owner");
        Event event = event(10L, owner);
        EventResponse eventResponse = mock(EventResponse.class);

        when(eventFinder.findCurrentUser(USER_EMAIL))
            .thenReturn(participant);
        when(eventFinder.findActiveEventById(event.getPublicId()))
            .thenReturn(event);

        when(loopedEventRepository.insertIfAbsent(
            any(UUID.class),
            any(LocalDateTime.class),
            eq(participant.getId()),
            eq(event.getId())
        )).thenReturn(1);

        when(loopedEventRepository.countByEventId(event.getId()))
            .thenReturn(4L);
        when(eventMapper.toResponse(event))
            .thenReturn(eventResponse);

        LoopedEventResponse result = handler.handle(
            new LoopInEventCommand(event.getPublicId(), USER_EMAIL)
        );

        assertSame(eventResponse, result.event());
        assertEquals(4L, result.loopedCount());

        verify(eventLoopInPolicy).requireLoopable(event);
        verify(loopedEventRepository).insertIfAbsent(
            any(UUID.class),
            any(LocalDateTime.class),
            eq(participant.getId()),
            eq(event.getId())
        );

        ArgumentCaptor<NotificationCommand> notificationCaptor =
            ArgumentCaptor.forClass(NotificationCommand.class);

        verify(notificationWriter).write(notificationCaptor.capture());

        NotificationCommand notification = notificationCaptor.getValue();

        assertSame(owner, notification.recipient());
        assertEquals(NotificationType.EVENT_LOOP_IN, notification.type());
        assertEquals(NotificationReferenceType.EVENT, notification.referenceType());
        assertEquals(event.getPublicId(), notification.referenceId());
        assertEquals(
            "event-loop-in:"
                + event.getPublicId()
                + ":"
                + participant.getPublicId(),
            notification.deduplicationKey()
        );
    }

    @Test
    void handle_ExistingLoopIn_DoesNotCreateDuplicateNotification() {
        User participant = user(1L, "Participant");
        User owner = user(2L, "Owner");
        Event event = event(10L, owner);
        EventResponse eventResponse = mock(EventResponse.class);

        when(eventFinder.findCurrentUser(USER_EMAIL))
            .thenReturn(participant);
        when(eventFinder.findActiveEventById(event.getPublicId()))
            .thenReturn(event);

        when(loopedEventRepository.insertIfAbsent(
            any(UUID.class),
            any(LocalDateTime.class),
            eq(participant.getId()),
            eq(event.getId())
        )).thenReturn(0);

        when(loopedEventRepository.countByEventId(event.getId()))
            .thenReturn(1L);
        when(eventMapper.toResponse(event))
            .thenReturn(eventResponse);

        LoopedEventResponse result = handler.handle(
            new LoopInEventCommand(event.getPublicId(), USER_EMAIL)
        );

        assertEquals(1L, result.loopedCount());
        verify(notificationWriter, never()).write(any());
    }

    @Test
    void handle_OwnerLoopsIntoOwnEvent_DoesNotNotifyOwner() {
        User owner = user(1L, "Owner");
        Event event = event(10L, owner);

        when(eventFinder.findCurrentUser(USER_EMAIL))
            .thenReturn(owner);
        when(eventFinder.findActiveEventById(event.getPublicId()))
            .thenReturn(event);

        when(loopedEventRepository.insertIfAbsent(
            any(UUID.class),
            any(LocalDateTime.class),
            eq(owner.getId()),
            eq(event.getId())
        )).thenReturn(1);

        when(loopedEventRepository.countByEventId(event.getId()))
            .thenReturn(1L);
        when(eventMapper.toResponse(event))
            .thenReturn(mock(EventResponse.class));

        handler.handle(
            new LoopInEventCommand(event.getPublicId(), USER_EMAIL)
        );

        verify(notificationWriter, never()).write(any());
    }

    @Test
    void handle_EventNotLoopable_DoesNotInsertOrNotify() {
        User participant = user(1L, "Participant");
        Event event = event(10L, user(2L, "Owner"));

        when(eventFinder.findCurrentUser(USER_EMAIL))
            .thenReturn(participant);
        when(eventFinder.findActiveEventById(event.getPublicId()))
            .thenReturn(event);

        doThrow(new InvalidEventStateException("Event cannot be looped into"))
            .when(eventLoopInPolicy)
            .requireLoopable(event);

        assertThrows(
            InvalidEventStateException.class,
            () -> handler.handle(
                new LoopInEventCommand(event.getPublicId(), USER_EMAIL)
            )
        );

        verifyNoInteractions(eventMapper);
        verify(notificationWriter, never()).write(any());
        verify(loopedEventRepository, never()).insertIfAbsent(
            any(),
            any(),
            any(),
            any()
        );
    }

    private User user(Long id, String name) {
        User user = new User(
            name.toLowerCase() + "@example.test",
            name,
            null
        );
        user.setId(id);
        user.setPublicId(UUID.randomUUID());
        return user;
    }

    private Event event(Long id, User owner) {
        Event event = new Event();
        event.setId(id);
        event.setPublicId(UUID.randomUUID());
        event.setTitle("Loopin Event");
        event.setOwner(owner);
        event.setStatus(EventStatus.PUBLISHED);
        event.setModerationStatus(ContentModerationStatus.APPROVED);
        event.setEndDateTime(LocalDateTime.now().plusDays(1));
        return event;
    }
}
