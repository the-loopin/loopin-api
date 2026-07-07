package com.loopin.api.service.implementation;

import com.loopin.api.common.enums.GroupStatus;
import com.loopin.api.entity.EventGroup;
import com.loopin.api.repository.EventGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupArchiveService {

    private final EventGroupRepository eventGroupRepository;

    @Transactional
    public int archiveGroupsForEvent(Long eventId) {
        List<EventGroup> activeGroups = eventGroupRepository.findByEventIdAndStatusIn(
                eventId,
                List.of(GroupStatus.OPEN, GroupStatus.FULL)
        );

        for (EventGroup group : activeGroups) {
            // Keep group messages for history/audit, but remove the group from active lifecycle flows.
            group.setStatus(GroupStatus.ARCHIVED);
        }

        eventGroupRepository.saveAll(activeGroups);
        return activeGroups.size();
    }
}
