package com.loopin.api.groups.api;

import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.enums.GroupStatus;
import com.loopin.api.groups.repository.EventGroupRepository;
import com.loopin.api.groups.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
class GroupLifecycleService implements GroupLifecycle {
    private final EventGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;

    public List<ArchivedGroupAwardRecipients> archiveActiveGroupsForEvent(Long eventId) {
        List<EventGroup> groups = groupRepository.findByEventIdAndStatusNot(eventId, GroupStatus.ARCHIVED);
        groups.forEach(group -> group.setStatus(GroupStatus.ARCHIVED));
        return groupRepository.saveAll(groups).stream()
                .map(group -> new ArchivedGroupAwardRecipients(
                        group.getAdmin(),
                        memberRepository.findByGroupId(group.getId()).stream()
                                .map(member -> member.getUser())
                                .toList()))
                .toList();
    }
}
