package com.loopin.api.events.listmyloopedevents;

import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.dto.response.LoopedEventResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.entity.UserLoopedEvent;
import com.loopin.api.events.mapper.EventMapper;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.events.repository.UserLoopedEventRepository;
import com.loopin.api.events.shared.finder.EventFinder;
import com.loopin.api.events.shared.loopin.LoopedEventCountLoader;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListMyLoopedEventsHandlerTest {

    private EventFinder eventFinder;
    private UserLoopedEventRepository loopedEventRepository;
    private EventRepository eventRepository;
    private LoopedEventCountLoader countLoader;
    private EventMapper eventMapper;

    private ListMyLoopedEventsHandler handler;

    @BeforeEach
    void setUp() {
        eventFinder = mock(EventFinder.class);
        loopedEventRepository = mock(UserLoopedEventRepository.class);
        eventRepository = mock(EventRepository.class);
        countLoader = mock(LoopedEventCountLoader.class);
        eventMapper = mock(EventMapper.class);

        handler = new ListMyLoopedEventsHandler(
            eventFinder,
            loopedEventRepository,
            eventRepository,
            countLoader,
            eventMapper
        );
    }

    @Test
    void handle_LoopedEvents_PreservesRelationOrderAndUsesBulkCounts() {
        String email = "user@example.test";
        Pageable pageable = PageRequest.of(0, 10);

        User user = new User(email, "User", null);
        user.setId(1L);

        Event newestEvent = event(101L, "Newest event");
        Event olderEvent = event(102L, "Older event");

        UserLoopedEvent newestRelation =
            new UserLoopedEvent(user, newestEvent);
        UserLoopedEvent olderRelation =
            new UserLoopedEvent(user, olderEvent);

        Page<UserLoopedEvent> relationPage = new PageImpl<>(
            List.of(newestRelation, olderRelation),
            pageable,
            2
        );

        EventResponse newestResponse = mock(EventResponse.class);
        EventResponse olderResponse = mock(EventResponse.class);

        when(eventFinder.findCurrentUser(email))
            .thenReturn(user);
        when(loopedEventRepository.findPageByUserId(user.getId(), pageable))
            .thenReturn(relationPage);

        // Repository result order intentionally reversed.
        when(eventRepository.findAllByIdWithInterests(
            List.of(101L, 102L)
        )).thenReturn(List.of(olderEvent, newestEvent));

        when(countLoader.load(List.of(101L, 102L)))
            .thenReturn(Map.of(
                101L, 7L,
                102L, 3L
            ));

        when(eventMapper.toResponse(newestEvent))
            .thenReturn(newestResponse);
        when(eventMapper.toResponse(olderEvent))
            .thenReturn(olderResponse);

        Page<LoopedEventResponse> result = handler.handle(
            new ListMyLoopedEventsQuery(email, pageable)
        );

        assertEquals(2, result.getContent().size());

        assertSame(
            newestResponse,
            result.getContent().get(0).event()
        );
        assertEquals(
            7L,
            result.getContent().get(0).loopedCount()
        );

        assertSame(
            olderResponse,
            result.getContent().get(1).event()
        );
        assertEquals(
            3L,
            result.getContent().get(1).loopedCount()
        );

        verify(countLoader).load(List.of(101L, 102L));
    }

    @Test
    void handle_MissingBulkCount_DefaultsToZero() {
        String email = "user@example.test";
        Pageable pageable = PageRequest.of(0, 10);

        User user = new User(email, "User", null);
        user.setId(1L);

        Event event = event(101L, "Event");
        UserLoopedEvent relation =
            new UserLoopedEvent(user, event);

        when(eventFinder.findCurrentUser(email))
            .thenReturn(user);
        when(loopedEventRepository.findPageByUserId(user.getId(), pageable))
            .thenReturn(new PageImpl<>(
                List.of(relation),
                pageable,
                1
            ));

        when(eventRepository.findAllByIdWithInterests(List.of(101L)))
            .thenReturn(List.of(event));
        when(countLoader.load(List.of(101L)))
            .thenReturn(Map.of());
        when(eventMapper.toResponse(event))
            .thenReturn(mock(EventResponse.class));

        Page<LoopedEventResponse> result = handler.handle(
            new ListMyLoopedEventsQuery(email, pageable)
        );

        assertEquals(
            0L,
            result.getContent().get(0).loopedCount()
        );
    }

    @Test
    void handle_EmptyPage_DoesNotLoadEventsOrCounts() {
        String email = "user@example.test";
        Pageable pageable = PageRequest.of(0, 10);

        User user = new User(email, "User", null);
        user.setId(1L);

        when(eventFinder.findCurrentUser(email))
            .thenReturn(user);
        when(loopedEventRepository.findPageByUserId(user.getId(), pageable))
            .thenReturn(Page.empty(pageable));

        Page<LoopedEventResponse> result = handler.handle(
            new ListMyLoopedEventsQuery(email, pageable)
        );

        assertEquals(0, result.getContent().size());

        verify(eventRepository, never())
            .findAllByIdWithInterests(org.mockito.ArgumentMatchers.anyList());

        verify(countLoader, never())
            .load(org.mockito.ArgumentMatchers.anyCollection());
    }

    private Event event(Long id, String title) {
        Event event = new Event();
        event.setId(id);
        event.setPublicId(UUID.randomUUID());
        event.setTitle(title);
        return event;
    }
}
