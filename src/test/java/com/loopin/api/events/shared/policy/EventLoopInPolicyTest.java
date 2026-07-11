package com.loopin.api.events.shared.policy;

import com.loopin.api.common.exception.InvalidEventStateException;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.moderation.enums.ContentModerationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventLoopInPolicyTest {

    private EventLoopInPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new EventLoopInPolicy();
    }

    @Test
    void requireLoopable_PublishedApprovedFutureEvent_DoesNotThrow() {
        Event event = validEvent();

        assertDoesNotThrow(() -> policy.requireLoopable(event));
    }

    @Test
    void requireLoopable_DraftEvent_ThrowsInvalidEventStateException() {
        Event event = validEvent();
        event.setStatus(EventStatus.DRAFT);

        assertThrows(
            InvalidEventStateException.class,
            () -> policy.requireLoopable(event)
        );
    }

    @Test
    void requireLoopable_CancelledEvent_ThrowsInvalidEventStateException() {
        Event event = validEvent();
        event.setStatus(EventStatus.CANCELLED);

        assertThrows(
            InvalidEventStateException.class,
            () -> policy.requireLoopable(event)
        );
    }

    @Test
    void requireLoopable_CompletedEvent_ThrowsInvalidEventStateException() {
        Event event = validEvent();
        event.setStatus(EventStatus.COMPLETED);

        assertThrows(
            InvalidEventStateException.class,
            () -> policy.requireLoopable(event)
        );
    }

    @Test
    void requireLoopable_PendingModeration_ThrowsInvalidEventStateException() {
        Event event = validEvent();
        event.setModerationStatus(ContentModerationStatus.PENDING_REVIEW);

        assertThrows(
            InvalidEventStateException.class,
            () -> policy.requireLoopable(event)
        );
    }

    @Test
    void requireLoopable_RejectedModeration_ThrowsInvalidEventStateException() {
        Event event = validEvent();
        event.setModerationStatus(ContentModerationStatus.REJECTED);

        assertThrows(
            InvalidEventStateException.class,
            () -> policy.requireLoopable(event)
        );
    }

    @Test
    void requireLoopable_ExpiredEvent_ThrowsInvalidEventStateException() {
        Event event = validEvent();
        event.setEndDateTime(LocalDateTime.now().minusMinutes(1));

        assertThrows(
            InvalidEventStateException.class,
            () -> policy.requireLoopable(event)
        );
    }

    private Event validEvent() {
        Event event = new Event();
        event.setStatus(EventStatus.PUBLISHED);
        event.setModerationStatus(ContentModerationStatus.APPROVED);
        event.setEndDateTime(LocalDateTime.now().plusDays(1));
        return event;
    }
}
