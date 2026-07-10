package com.loopin.api.events.cancel;

import com.loopin.api.events.entity.Event;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.events.shared.access.EventAccessPolicy;
import com.loopin.api.events.shared.finder.EventFinder;
import com.loopin.api.events.shared.notification.EventMemberNotifier;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CancelEventHandlerTest {

    @Test
    void handle_cancelsAllowedEventAndNotifiesMembers() {
        EventRepository repository = mock(EventRepository.class);
        EventFinder finder = mock(EventFinder.class);
        EventAccessPolicy accessPolicy = mock(EventAccessPolicy.class);
        EventMemberNotifier memberNotifier = mock(EventMemberNotifier.class);
        CancelEventHandler handler = new CancelEventHandler(repository, finder, accessPolicy, memberNotifier);
        UUID id = UUID.randomUUID();
        Event event = event(id, EventStatus.PUBLISHED);
        User user = new User("owner@loopin.test", "Owner", null);
        when(finder.findCurrentUser("owner@loopin.test")).thenReturn(user);
        when(finder.findActiveEventById(id)).thenReturn(event);

        handler.handle(new CancelEventCommand(id, "owner@loopin.test"));

        assertEquals(EventStatus.CANCELLED, event.getStatus());
        verify(accessPolicy).requireOwnerOrAdmin(event, user);
        verify(memberNotifier).notifyMembers(event, "Event cancelled", "\"Event\" has been cancelled.");
        verify(repository).save(event);
    }

    @Test
    void handle_rejectsCancellationOfCompletedEvent() {
        EventRepository repository = mock(EventRepository.class);
        EventFinder finder = mock(EventFinder.class);
        EventAccessPolicy accessPolicy = mock(EventAccessPolicy.class);
        EventMemberNotifier memberNotifier = mock(EventMemberNotifier.class);
        CancelEventHandler handler = new CancelEventHandler(repository, finder, accessPolicy, memberNotifier);
        UUID id = UUID.randomUUID();
        Event event = event(id, EventStatus.COMPLETED);
        when(finder.findCurrentUser("owner@loopin.test")).thenReturn(new User("owner@loopin.test", "Owner", null));
        when(finder.findActiveEventById(id)).thenReturn(event);

        assertThrows(IllegalArgumentException.class,
                () -> handler.handle(new CancelEventCommand(id, "owner@loopin.test")));
        verify(repository, never()).save(event);
        verify(memberNotifier, never()).notifyMembers(event, "Event cancelled", "\"Event\" has been cancelled.");
    }

    private Event event(UUID id, EventStatus status) {
        Event event = new Event();
        event.setPublicId(id);
        event.setTitle("Event");
        event.setStatus(status);
        return event;
    }
}
