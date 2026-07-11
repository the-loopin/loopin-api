package com.loopin.api.events.listmyloopedevents;

import org.springframework.data.domain.Pageable;

public record ListMyLoopedEventsQuery(
    String currentUsername,
    Pageable pageable
) {
}
