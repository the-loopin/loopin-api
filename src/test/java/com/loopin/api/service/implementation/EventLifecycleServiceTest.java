package com.loopin.api.service.implementation;

import com.loopin.api.common.enums.EventStatus;
import com.loopin.api.entity.Event;
import com.loopin.api.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventLifecycleServiceTest {

    private EventRepository eventRepository;
    private GroupArchiveService groupArchiveService;
    private EventLifecycleService eventLifecycleService;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        groupArchiveService = mock(GroupArchiveService.class);
        eventLifecycleService = new EventLifecycleService(eventRepository, groupArchiveService);
    }

    @Test
    void completeFinishedPublishedEvents_CompletesEventsAndArchivesGroups() {
        Event finishedEvent = new Event();
        finishedEvent.setId(10L);
        finishedEvent.setStatus(EventStatus.PUBLISHED);

        when(eventRepository.findByStatusAndEndDateTimeBeforeAndDeletedAtIsNull(
                eq(EventStatus.PUBLISHED),
                any(LocalDateTime.class)
        )).thenReturn(List.of(finishedEvent));

        int completedCount = eventLifecycleService.completeFinishedPublishedEvents();

        assertEquals(1, completedCount);
        assertEquals(EventStatus.COMPLETED, finishedEvent.getStatus());
        verify(groupArchiveService).archiveGroupsForEvent(10L);
        verify(eventRepository).saveAll(List.of(finishedEvent));
    }
}
