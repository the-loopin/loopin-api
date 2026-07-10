package com.loopin.api.groups.api;

import com.loopin.api.groups.entity.GroupMember;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
class GroupMemberLookupService implements GroupMemberLookup {
    private final GroupMemberRepository groupMemberRepository;
    public List<User> findActiveUsersByEventId(Long eventId) {
        return groupMemberRepository.findDistinctActiveUsersByEventId(eventId);
    }
    public List<GroupMember> findMembersByGroupId(Long groupId) {
        return groupMemberRepository.findByGroupId(groupId);
    }
}
