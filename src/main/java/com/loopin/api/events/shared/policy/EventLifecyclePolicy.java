package com.loopin.api.events.shared.policy;

import com.loopin.api.common.exception.InvalidEventStateException;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.enums.EventStatus;

/**
 * Centralizes lifecycle status decisions while command handlers are introduced incrementally.
 *
 * <h2>Allowed lifecycle transitions</h2>
 * <pre>
 * DRAFT      ──(moderation approve)──► PUBLISHED
 * DRAFT      ──(cancel command)───────► CANCELLED
 * PUBLISHED  ──(moderation reject)───► DRAFT
 * PUBLISHED  ──(cancel command)───────► CANCELLED
 * PUBLISHED  ──(completion job)───────► COMPLETED
 * CANCELLED  ──(terminal – no further transitions)
 * COMPLETED  ──(terminal – no further transitions)
 * </pre>
 *
 * <p>Normal create and update requests never supply a {@code status} field.
 * The backend assigns the initial status ({@link #initialStatus()}); content
 * requiring moderation is moved to {@code DRAFT}.  Lifecycle changes are
 * issued as separate commands: {@code CancelEventCommand},
 * {@code ApproveEventModerationCommand}, and {@code RejectEventModerationCommand}.
 */
public final class EventLifecyclePolicy {

    private EventLifecyclePolicy() {
    }

    /**
     * The status assigned to a new event before automated moderation is evaluated.
     * Automatically approved content receives {@code PUBLISHED}; content requiring
     * human review is moved to {@code DRAFT} by {@link #markPendingModeration(Event)}.
     */
    public static EventStatus initialStatus() {
        return EventStatus.PUBLISHED;
    }

    /**
     * Asserts that the event may still be mutated through the normal update command.
     * Events in terminal states ({@code CANCELLED}, {@code COMPLETED}) cannot be
     * edited and throw {@link InvalidEventStateException}.
     *
     * @throws InvalidEventStateException when the event status is {@code CANCELLED}
     *                                    or {@code COMPLETED}
     */
    public static void requireEditable(Event event) {
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new InvalidEventStateException(
                    "Event cannot be updated: it has been cancelled.");
        }
        if (event.getStatus() == EventStatus.COMPLETED) {
            throw new InvalidEventStateException(
                    "Event cannot be updated: it has already been completed.");
        }
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

    /**
     * Transitions the event to {@code CANCELLED}.
     *
     * @throws IllegalArgumentException when the event is already {@code CANCELLED}
     *                                  or is {@code COMPLETED}
     */
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
