package com.loopin.api.service.implementation;

import com.loopin.api.common.enums.EventStatus;
import com.loopin.api.entity.Event;
import com.loopin.api.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventLifecycleService {

    private final EventRepository eventRepository;
    private final GroupArchiveService groupArchiveService;

    @Scheduled(
            fixedDelayString = "${loopin.event-lifecycle.complete-finished-delay-ms:300000}",
            initialDelayString = "${loopin.event-lifecycle.complete-finished-initial-delay-ms:60000}"
    )
    @Transactional
    public int completeFinishedPublishedEvents() {
        List<Event> finishedEvents = eventRepository.findByStatusAndEndDateTimeBeforeAndDeletedAtIsNull(
                EventStatus.PUBLISHED,
                LocalDateTime.now()
        );

        for (Event event : finishedEvents) {
            event.setStatus(EventStatus.COMPLETED);
            groupArchiveService.archiveGroupsForEvent(event.getId());
        }

        eventRepository.saveAll(finishedEvents);

        if (!finishedEvents.isEmpty()) {
            log.info("Completed {} finished events and archived their groups", finishedEvents.size());
        }

        return finishedEvents.size();
    }
}
