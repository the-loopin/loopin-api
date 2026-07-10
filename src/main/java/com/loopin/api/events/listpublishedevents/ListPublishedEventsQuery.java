package com.loopin.api.events.listpublishedevents;

import com.loopin.api.events.enums.EventCategory;
import com.loopin.api.events.enums.EventType;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * The complete, cacheable input to the public event listing read model.
 */
public record ListPublishedEventsQuery(
        EventType type,
        EventCategory category,
        String city,
        Boolean isFree,
        String search,
        LocalDate startDate,
        LocalDate endDate,
        Pageable pageable
) {
}
