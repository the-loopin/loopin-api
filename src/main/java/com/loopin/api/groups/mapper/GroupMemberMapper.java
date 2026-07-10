package com.loopin.api.groups.mapper;

import com.loopin.api.groups.dto.response.GroupMemberResponse;
import com.loopin.api.groups.entity.GroupMember;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class GroupMemberMapper {

    public GroupMemberResponse toResponse(GroupMember member) {
        if (member == null) {
            return null;
        }

        return GroupMemberResponse.from(member);
    }

    public List<GroupMemberResponse> toResponseList(List<GroupMember> members) {
        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }

        return members.stream()
                .map(this::toResponse)
                .toList();
    }
}
