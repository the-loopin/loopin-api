package com.loopin.api.events.listpublishedevents;

import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.enums.EventCategory;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.events.enums.EventType;
import com.loopin.api.events.mapper.EventMapper;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.events.shared.specification.PublishedEventSpecifications;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListPublishedEventsHandlerTest {

    @Test
    void handle_preservesPaginationAndLoadsPageInterestsInListingOrder() {
        EventRepository repository = mock(EventRepository.class);
        EventMapper mapper = mock(EventMapper.class);

        ListPublishedEventsHandler handler = new ListPublishedEventsHandler(
            repository,
            mapper,
            new PublishedEventSpecifications()
        );

        Event first = event(1L, "First");
        Event second = event(2L, "Second");

        PageRequest pageable = PageRequest.of(1, 2);

        when(repository.findAll(
            any(Specification.class),
            eq(pageable)
        )).thenReturn(
            new PageImpl<>(
                List.of(first, second),
                pageable,
                5
            )
        );

        when(repository.findPublishedByIdInWithInterests(
            List.of(1L, 2L)
        )).thenReturn(
            List.of(second, first)
        );

        EventResponse firstResponse = mock(EventResponse.class);
        EventResponse secondResponse = mock(EventResponse.class);

        when(mapper.toResponse(first))
            .thenReturn(firstResponse);

        when(mapper.toResponse(second))
            .thenReturn(secondResponse);

        CachedEventPage cachedResult = handler.handle(
            new ListPublishedEventsQuery(
                EventType.EVENT,
                EventCategory.TECH,
                "Baku",
                false,
                "meetup",
                null,
                null,
                pageable
            )
        );

        Page<EventResponse> result =
            cachedResult.toPage(pageable);

        assertEquals(
            List.of(firstResponse, secondResponse),
            result.getContent()
        );

        assertEquals(
            5,
            result.getTotalElements()
        );

        assertEquals(
            1,
            result.getNumber()
        );
    }

    private Event event(Long id, String title) {
        Event event = new Event();
        event.setId(id);
        event.setTitle(title);
        event.setStatus(EventStatus.PUBLISHED);
        return event;
    }
}
