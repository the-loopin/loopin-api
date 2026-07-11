package com.loopin.api.events.dto.response;

public record LoopedEventResponse(
    EventResponse event,
    long loopedCount
) {
}
