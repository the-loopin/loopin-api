package com.loopin.api.groups.listgroupmembers;

import com.loopin.api.groups.dto.response.GroupMemberResponse;
import com.loopin.api.groups.mapper.GroupMemberMapper;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.groups.shared.finder.GroupFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ListGroupMembersHandler {
    private final GroupFinder groupFinder;
    private final GroupMemberRepository memberRepository;
    private final GroupMemberMapper memberMapper;

    @Transactional(readOnly = true)
    public List<GroupMemberResponse> handle(ListGroupMembersQuery query) {
        return memberMapper.toResponseList(memberRepository.findByGroupId(groupFinder.findGroup(query.groupId()).getId()));
    }
}
