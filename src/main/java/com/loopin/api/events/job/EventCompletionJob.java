package com.loopin.api.events.job;

import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.events.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventCompletionJob {

    private static final String LOCK_NAME = "event-completion";

    private final EventRepository eventRepository;
    private final EventCompletionProcessor eventCompletionProcessor;
    private final EventCompletionJobLockService eventCompletionJobLockService;

    @Value("${loopin.jobs.event-completion.lock-at-most-ms:1800000}")
    private long lockAtMostMs;

    @Scheduled(
            fixedDelayString = "${loopin.jobs.event-completion.fixed-delay-ms:3600000}",
            initialDelayString = "${loopin.jobs.event-completion.initial-delay-ms:60000}")
    public void completePassedEvents() {
        completePassedEvents(LocalDateTime.now());
    }

    public void completePassedEvents(LocalDateTime now) {
        if (!eventCompletionJobLockService.acquireLock(
                LOCK_NAME,
                now,
                Duration.ofMillis(lockAtMostMs))) {
            log.info("Skipping event completion job because another instance holds the lock.");
            return;
        }

        int completedEvents = 0;
        int archivedGroups = 0;
        List<Long> passedEventIds = List.of();

        try {
            log.info("Starting event completion job.");
            passedEventIds = eventRepository
                    .findIdsByStatusAndEndDateTimeBeforeAndDeletedAtIsNull(EventStatus.PUBLISHED, now);

            for (Long eventId : passedEventIds) {
                try {
                    EventCompletionResult result = eventCompletionProcessor.completeEvent(eventId);
                    if (result.completed()) {
                        completedEvents++;
                        archivedGroups += result.archivedGroups();
                    }
                } catch (RuntimeException ex) {
                    log.warn("Failed to complete event {} during event completion job type={}",
                            eventId, ex.getClass().getSimpleName());
                }
            }
        } finally {
            eventCompletionJobLockService.releaseLock(LOCK_NAME, LocalDateTime.now());
        }

        log.info(
                "Finished event completion job. Candidate events: {}, completed events: {}, archived groups: {}.",
                passedEventIds.size(),
                completedEvents,
                archivedGroups);
    }
}
