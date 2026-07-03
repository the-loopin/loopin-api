package com.loopin.api.mapper;

import com.loopin.api.dto.group.response.GroupMemberResponse;
import com.loopin.api.entity.GroupMember;
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
