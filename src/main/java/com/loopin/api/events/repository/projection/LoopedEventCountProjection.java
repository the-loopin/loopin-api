package com.loopin.api.events.repository.projection;

public interface LoopedEventCountProjection {

    Long getEventId();

    Long getLoopedCount();
}
