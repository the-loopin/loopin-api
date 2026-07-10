package com.loopin.api.groups.getmembershipdetails;

import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.groups.dto.response.GroupMemberResponse;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.mapper.GroupMemberMapper;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.groups.shared.finder.GroupFinder;
import com.loopin.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GetMembershipDetailsHandler {
    private final GroupFinder groupFinder;
    private final GroupMemberRepository memberRepository;
    private final GroupMemberMapper memberMapper;

    @Transactional(readOnly = true)
    public GroupMemberResponse handle(GetMembershipDetailsQuery query) {
        EventGroup group = groupFinder.findGroup(query.groupId());
        User user = groupFinder.findActiveUser(query.userId());
        return memberMapper.toResponse(memberRepository.findByGroupIdAndUserId(group.getId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Group membership not found for group " + query.groupId() + " and user " + query.userId())));
    }
}
