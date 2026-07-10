package com.loopin.api.events.getpublishedbyid;

import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.mapper.EventMapper;
import com.loopin.api.events.repository.EventRepository;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetPublishedEventByIdHandlerTest {

    private final EventRepository eventRepository = mock(EventRepository.class);
    private final EventMapper eventMapper = mock(EventMapper.class);
    private final GetPublishedEventByIdHandler handler = new GetPublishedEventByIdHandler(eventRepository, eventMapper);

    @Test
    void handle_PublishedEventExists_ReturnsMappedResponseWithFetchedInterests() {
        UUID eventId = UUID.randomUUID();
        GetPublishedEventByIdQuery query = new GetPublishedEventByIdQuery(eventId);
        Event event = new Event();
        EventResponse response = mock(EventResponse.class);
        when(eventRepository.findPublishedByPublicIdWithInterests(eventId)).thenReturn(Optional.of(event));
        when(eventMapper.toResponse(event)).thenReturn(response);

        EventResponse result = handler.handle(query);

        assertEquals(response, result);
        verify(eventRepository).findPublishedByPublicIdWithInterests(eventId);
        verify(eventMapper).toResponse(event);
    }

    @Test
    void handle_PublishedEventDoesNotExist_ThrowsNotFoundException() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findPublishedByPublicIdWithInterests(eventId)).thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> handler.handle(new GetPublishedEventByIdQuery(eventId))
        );
    }
}
