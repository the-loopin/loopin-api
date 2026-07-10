package com.loopin.api.events.job;

import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.notifications.enums.NotificationReferenceType;
import com.loopin.api.notifications.enums.NotificationType;
import com.loopin.api.events.entity.Event;
import com.loopin.api.users.entity.User;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.notifications.service.NotificationService;
import com.loopin.api.notifications.service.NotificationCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EventReminderJob {

    private static final String LOCK_NAME = "event-reminders";

    private final EventRepository eventRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final NotificationService notificationService;
    private final EventCompletionJobLockService jobLockService;

    @Scheduled(
            fixedDelayString = "${loopin.notifications.reminders.fixed-delay-ms:300000}",
            initialDelayString = "${loopin.notifications.reminders.initial-delay-ms:60000}")
    public void createReminders() {
        LocalDateTime now = LocalDateTime.now();
        if (!jobLockService.acquireLock(LOCK_NAME, now, Duration.ofMinutes(10))) {
            return;
        }
        try {
            List<Event> events = eventRepository
                    .findByStatusAndStartDateTimeAfterAndStartDateTimeLessThanEqualAndDeletedAtIsNull(
                            EventStatus.PUBLISHED, now, now.plusHours(24));
            for (Event event : events) {
                for (User recipient : groupMemberRepository.findDistinctActiveUsersByEventId(event.getId())) {
                    String deduplicationKey = "event-reminder-24h:"
                            + event.getPublicId() + ":" + recipient.getPublicId();
                    notificationService.create(new NotificationCommand(
                            recipient,
                            NotificationType.EVENT_REMINDER,
                            "Event starts soon",
                            event.getTitle() + " starts within 24 hours.",
                            NotificationReferenceType.EVENT,
                            event.getPublicId(),
                            deduplicationKey));
                }
            }
        } finally {
            jobLockService.releaseLock(LOCK_NAME, LocalDateTime.now());
        }
    }
}
