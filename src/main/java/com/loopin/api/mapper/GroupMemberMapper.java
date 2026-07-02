package com.loopin.api.mapper;

import com.loopin.api.dto.group.request.GroupMemberRequest;
import com.loopin.api.dto.group.response.GroupMemberResponse;
import com.loopin.api.entity.EventGroup;
import com.loopin.api.entity.GroupMember;
import com.loopin.api.entity.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
                .collect(Collectors.toList());
    }

    // 3. Request DTO -> Entity
    public GroupMember toEntity(GroupMemberRequest request, EventGroup group, User user) {
        if (request == null && group == null && user == null) {
            return null;
        }

        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);
        member.setJoinedAt(LocalDateTime.now());

        return member;
    }
}