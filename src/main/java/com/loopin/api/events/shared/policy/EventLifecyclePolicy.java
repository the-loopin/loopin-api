package com.loopin.api.events.shared.policy;

import com.loopin.api.events.entity.Event;
import com.loopin.api.events.enums.EventStatus;

/**
 * Centralizes lifecycle status decisions while command handlers are introduced incrementally.
 */
public final class EventLifecyclePolicy {

    private EventLifecyclePolicy() {
    }

    /**
     * The status assigned to a new event before automated moderation is evaluated.
     */
    public static EventStatus initialStatus() {
        return EventStatus.PUBLISHED;
    }

    public static void markPendingModeration(Event event) {
        event.setStatus(EventStatus.DRAFT);
    }

    public static void approveModeration(Event event) {
        event.setStatus(EventStatus.PUBLISHED);
    }

    public static void rejectModeration(Event event) {
        event.setStatus(EventStatus.DRAFT);
    }

    public static void cancel(Event event) {
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new IllegalArgumentException("Event is already cancelled");
        }
        if (event.getStatus() == EventStatus.COMPLETED) {
            throw new IllegalArgumentException("Completed events cannot be cancelled");
        }
        event.setStatus(EventStatus.CANCELLED);
    }
}
