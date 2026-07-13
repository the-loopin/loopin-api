package com.loopin.api.events.listpublishedevents;

import com.loopin.api.events.dto.response.EventResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class CachedEventPageTest {

    @Test
    void toPage_reconstructsPageUsingProvidedPageable() {
        EventResponse first = new EventResponse();
        EventResponse second = new EventResponse();

        CachedEventPage cachedPage = new CachedEventPage(
            List.of(first, second),
            5
        );

        PageRequest pageable = PageRequest.of(1, 2);

        Page<EventResponse> result =
            cachedPage.toPage(pageable);

        assertEquals(
            List.of(first, second),
            result.getContent()
        );

        assertEquals(5, result.getTotalElements());
        assertEquals(1, result.getNumber());
        assertEquals(2, result.getSize());
    }

    @Test
    void constructor_copiesProvidedContent() {
        EventResponse event = new EventResponse();

        List<EventResponse> original =
            new java.util.ArrayList<>(List.of(event));

        CachedEventPage cachedPage =
            new CachedEventPage(original, 1);

        assertNotSame(
            original,
            cachedPage.getContent()
        );

        assertEquals(
            List.of(event),
            cachedPage.getContent()
        );
    }
}
