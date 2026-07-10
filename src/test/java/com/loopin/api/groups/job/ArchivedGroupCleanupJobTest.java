package com.loopin.api.groups.job;

import com.loopin.api.groups.enums.GroupStatus;
import com.loopin.api.events.entity.EventGroup;
import com.loopin.api.events.repository.EventGroupRepository;
import com.loopin.api.groups.repository.GroupJoinRequestRepository;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.chat.repository.GroupMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchivedGroupCleanupJobTest {

    private EventGroupRepository eventGroupRepository;
    private GroupMessageRepository groupMessageRepository;
    private GroupMemberRepository groupMemberRepository;
    private GroupJoinRequestRepository groupJoinRequestRepository;
    private ArchivedGroupCleanupJob cleanupJob;

    @BeforeEach
    void setUp() {
        eventGroupRepository = mock(EventGroupRepository.class);
        groupMessageRepository = mock(GroupMessageRepository.class);
        groupMemberRepository = mock(GroupMemberRepository.class);
        groupJoinRequestRepository = mock(GroupJoinRequestRepository.class);
        cleanupJob = new ArchivedGroupCleanupJob(
                eventGroupRepository,
                groupMessageRepository,
                groupMemberRepository,
                groupJoinRequestRepository);
        ReflectionTestUtils.setField(cleanupJob, "archivedCleanupDays", 30L);
    }

    @Test
    void deleteExpiredArchivedGroups_DeletesMessagesMembersJoinRequestsAndGroup() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 7, 12, 0);
        EventGroup group = new EventGroup();
        group.setId(10L);
        group.setStatus(GroupStatus.ARCHIVED);

        when(eventGroupRepository.findByStatusAndUpdatedAtBefore(GroupStatus.ARCHIVED, now.minusDays(30)))
                .thenReturn(List.of(group));

        int deletedGroups = cleanupJob.deleteExpiredArchivedGroups(now);

        assertEquals(1, deletedGroups);
        verify(groupMessageRepository).deleteByGroupId(10L);
        verify(groupMemberRepository).deleteByGroupId(10L);
        verify(groupJoinRequestRepository).deleteByGroupId(10L);
        verify(eventGroupRepository).delete(group);
    }

    @Test
    void deleteExpiredArchivedGroups_NoEligibleGroups_DoesNothing() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 7, 12, 0);
        when(eventGroupRepository.findByStatusAndUpdatedAtBefore(GroupStatus.ARCHIVED, now.minusDays(30)))
                .thenReturn(List.of());

        int deletedGroups = cleanupJob.deleteExpiredArchivedGroups(now);

        assertEquals(0, deletedGroups);
        verify(groupMessageRepository, never()).deleteByGroupId(10L);
        verify(groupMemberRepository, never()).deleteByGroupId(10L);
        verify(groupJoinRequestRepository, never()).deleteByGroupId(10L);
    }
}
