package com.loopin.api.groups.listmygroups;

import com.loopin.api.groups.dto.response.GroupResponse;
import com.loopin.api.groups.mapper.GroupMapper;
import com.loopin.api.groups.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListMyGroupsHandler {

    private final GroupMemberRepository memberRepository;
    private final GroupMapper groupMapper;

    public List<GroupResponse> handle(
        ListMyGroupsQuery query
    ) {
        return memberRepository
            .findGroupsByUserEmail(
                query.currentUsername()
            )
            .stream()
            .map(groupMapper::toGroupResponse)
            .toList();
    }
}
