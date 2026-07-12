package com.loopin.api.events.job;

import com.loopin.api.users.enums.BadgeType;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.events.entity.Event;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.entity.UserBadge;
import com.loopin.api.groups.api.ArchivedGroupAwardRecipients;
import com.loopin.api.groups.api.GroupLifecycle;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.users.repository.UserBadgeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventCompletionProcessorTest {

    private EventRepository eventRepository;
    private GroupLifecycle groupLifecycle;
    private UserBadgeRepository userBadgeRepository;
    private EventCompletionProcessor eventCompletionProcessor;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        groupLifecycle = mock(GroupLifecycle.class);
        userBadgeRepository = mock(UserBadgeRepository.class);
        eventCompletionProcessor = new EventCompletionProcessor(
                eventRepository,
                groupLifecycle,
                userBadgeRepository);
    }

    @Test
    void completeEvent_CompletesEventArchivesGroupsAndAssignsBadges() {
        Event event = new Event();
        event.setId(1L);
        event.setStatus(EventStatus.PUBLISHED);

        User creator = user(10L, "creator@email.com");
        User attendee = user(20L, "attendee@email.com");
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(groupLifecycle.archiveActiveGroupsForEvent(1L))
                .thenReturn(List.of(new ArchivedGroupAwardRecipients(creator, List.of(creator, attendee))));

        EventCompletionResult result = eventCompletionProcessor.completeEvent(1L);

        assertEquals(true, result.completed());
        assertEquals(1, result.archivedGroups());
        assertEquals(EventStatus.COMPLETED, event.getStatus());
        verify(eventRepository).save(event);

        ArgumentCaptor<UserBadge> badgeCaptor = ArgumentCaptor.forClass(UserBadge.class);
        verify(userBadgeRepository, org.mockito.Mockito.times(3)).save(badgeCaptor.capture());
        List<UserBadge> badges = badgeCaptor.getAllValues();

        assertEquals(BadgeType.GROUP_CREATOR, badges.get(0).getBadgeType());
        assertEquals(creator, badges.get(0).getUser());
        assertEquals(BadgeType.EVENT_ATTENDEE, badges.get(1).getBadgeType());
        assertEquals(creator, badges.get(1).getUser());
        assertEquals(BadgeType.EVENT_ATTENDEE, badges.get(2).getBadgeType());
        assertEquals(attendee, badges.get(2).getUser());
    }

    @Test
    void completeEvent_DoesNotCreateDuplicateBadges() {
        Event event = new Event();
        event.setId(1L);
        event.setStatus(EventStatus.PUBLISHED);

        User creator = user(10L, "creator@email.com");
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(groupLifecycle.archiveActiveGroupsForEvent(1L))
                .thenReturn(List.of(new ArchivedGroupAwardRecipients(creator, List.of(creator))));
        when(userBadgeRepository.existsByUserIdAndBadgeType(10L, BadgeType.GROUP_CREATOR))
                .thenReturn(true);
        when(userBadgeRepository.existsByUserIdAndBadgeType(10L, BadgeType.EVENT_ATTENDEE))
                .thenReturn(true);

        eventCompletionProcessor.completeEvent(1L);

        verify(userBadgeRepository, never()).save(any(UserBadge.class));
    }

    @Test
    void completeEvent_IgnoresConcurrentDuplicateBadgeInsert() {
        Event event = new Event();
        event.setId(1L);
        event.setStatus(EventStatus.PUBLISHED);

        User creator = user(10L, "creator@email.com");
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(groupLifecycle.archiveActiveGroupsForEvent(1L))
                .thenReturn(List.of(new ArchivedGroupAwardRecipients(creator, List.of())));
        when(userBadgeRepository.save(any(UserBadge.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate badge"));

        EventCompletionResult result = eventCompletionProcessor.completeEvent(1L);

        assertEquals(true, result.completed());
        assertEquals(1, result.archivedGroups());
    }

    private User user(Long id, String email) {
        User user = new User(email, "Test User", null);
        user.setId(id);
        return user;
    }
}
