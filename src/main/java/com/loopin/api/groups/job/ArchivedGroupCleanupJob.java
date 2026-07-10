package com.loopin.api.groups.job;

import com.loopin.api.groups.enums.GroupStatus;
import com.loopin.api.events.entity.EventGroup;
import com.loopin.api.events.repository.EventGroupRepository;
import com.loopin.api.groups.repository.GroupJoinRequestRepository;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.chat.repository.GroupMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArchivedGroupCleanupJob {

    private final EventGroupRepository eventGroupRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupJoinRequestRepository groupJoinRequestRepository;

    @Value("${loopin.group-retention.archived-cleanup-days:30}")
    private long archivedCleanupDays;

    @Scheduled(
            fixedDelayString = "${loopin.group-retention.cleanup-fixed-delay-ms:86400000}",
            initialDelayString = "${loopin.group-retention.cleanup-initial-delay-ms:300000}")
    public void deleteExpiredArchivedGroups() {
        deleteExpiredArchivedGroups(LocalDateTime.now());
    }

    @Transactional
    public int deleteExpiredArchivedGroups(LocalDateTime now) {
        LocalDateTime cutoff = now.minusDays(archivedCleanupDays);
        List<EventGroup> expiredGroups =
                eventGroupRepository.findByStatusAndUpdatedAtBefore(GroupStatus.ARCHIVED, cutoff);

        for (EventGroup group : expiredGroups) {
            Long groupId = group.getId();
            groupMessageRepository.deleteByGroupId(groupId);
            groupMemberRepository.deleteByGroupId(groupId);
            groupJoinRequestRepository.deleteByGroupId(groupId);
            eventGroupRepository.delete(group);
        }

        if (!expiredGroups.isEmpty()) {
            log.info("Deleted {} archived groups older than {} days.", expiredGroups.size(), archivedCleanupDays);
        }

        return expiredGroups.size();
    }
}
