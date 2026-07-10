package com.loopin.api.groups.api;

import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.enums.GroupStatus;
import com.loopin.api.groups.repository.EventGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
class GroupLifecycleService implements GroupLifecycle {
    private final EventGroupRepository groupRepository;
    public List<EventGroup> archiveActiveGroupsForEvent(Long eventId) {
        List<EventGroup> groups = groupRepository.findByEventIdAndStatusNot(eventId, GroupStatus.ARCHIVED);
        groups.forEach(group -> group.setStatus(GroupStatus.ARCHIVED));
        return groupRepository.saveAll(groups);
    }
}
