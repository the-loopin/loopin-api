package com.loopin.api.core.events.job;

import com.loopin.api.core.events.enums.EventStatus;
import com.loopin.api.core.events.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventCompletionJobTest {

    private EventRepository eventRepository;
    private EventCompletionProcessor eventCompletionProcessor;
    private EventCompletionJobLockService eventCompletionJobLockService;
    private EventCompletionJob eventCompletionJob;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        eventCompletionProcessor = mock(EventCompletionProcessor.class);
        eventCompletionJobLockService = mock(EventCompletionJobLockService.class);
        eventCompletionJob = new EventCompletionJob(
                eventRepository,
                eventCompletionProcessor,
                eventCompletionJobLockService);
    }

    @Test
    void completePassedEvents_SkipsWhenLockIsHeld() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 5, 12, 0);
        when(eventCompletionJobLockService.acquireLock(eq("event-completion"), eq(now), any(Duration.class)))
                .thenReturn(false);

        eventCompletionJob.completePassedEvents(now);

        verify(eventRepository, never())
                .findIdsByStatusAndEndDateTimeBeforeAndDeletedAtIsNull(any(), any());
        verify(eventCompletionProcessor, never()).completeEvent(any());
        verify(eventCompletionJobLockService, never()).releaseLock(eq("event-completion"), any());
    }

    @Test
    void completePassedEvents_ContinuesWhenSingleEventFailsAndReleasesLock() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 5, 12, 0);
        when(eventCompletionJobLockService.acquireLock(eq("event-completion"), eq(now), any(Duration.class)))
                .thenReturn(true);
        when(eventRepository.findIdsByStatusAndEndDateTimeBeforeAndDeletedAtIsNull(EventStatus.PUBLISHED, now))
                .thenReturn(List.of(1L, 2L));
        when(eventCompletionProcessor.completeEvent(1L)).thenThrow(new RuntimeException("bad event"));
        when(eventCompletionProcessor.completeEvent(2L)).thenReturn(new EventCompletionResult(true, 2));

        eventCompletionJob.completePassedEvents(now);

        verify(eventCompletionProcessor).completeEvent(1L);
        verify(eventCompletionProcessor).completeEvent(2L);
        verify(eventCompletionJobLockService).releaseLock(eq("event-completion"), any(LocalDateTime.class));
    }
}
