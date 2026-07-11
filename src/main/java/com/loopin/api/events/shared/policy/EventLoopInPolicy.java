package com.loopin.api.events.shared.policy;

import com.loopin.api.common.exception.InvalidEventStateException;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.moderation.enums.ContentModerationStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class EventLoopInPolicy {

    public void requireLoopable(Event event) {
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new InvalidEventStateException(
                "Only published events can be looped into."
            );
        }

        if (event.getModerationStatus()
            != ContentModerationStatus.APPROVED) {
            throw new InvalidEventStateException(
                "Event is not approved for public participation."
            );
        }

        if (!event.getEndDateTime().isAfter(LocalDateTime.now())) {
            throw new InvalidEventStateException(
                "Completed events cannot be looped into."
            );
        }
    }
}
