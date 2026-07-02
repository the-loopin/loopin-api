package com.loopin.api.mapper;


import com.loopin.api.dto.group.request.CreateGroupRequest;
import com.loopin.api.dto.group.response.GroupResponse;
import com.loopin.api.entity.Event;
import com.loopin.api.entity.EventGroup;
import com.loopin.api.entity.User;
import com.loopin.api.common.enums.GroupStatus;
import com.loopin.api.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GroupMapper {

    private final GroupMemberRepository groupMemberRepository;

    public EventGroup toEntity(CreateGroupRequest request, User admin, Event event) {
        EventGroup group = new EventGroup();
        group.setEvent(event);
        group.setAdmin(admin);
        group.setTitle(request.getTitle());
        group.setGroupSize(request.getGroupSize());
        group.setMaxMembers(request.getMaxMembers());
        group.setGroupNote(request.getGroupNote());
        group.setStatus(GroupStatus.OPEN);
        return group;
    }

    public GroupResponse toGroupResponse(EventGroup group) {
        int memberCount = groupMemberRepository.countByGroupId(group.getId());
        return new GroupResponse(
                group.getId(),
                group.getEvent() != null ? group.getEvent().getId() : null,
                group.getAdmin().getId(),
                group.getAdmin().getEmail(),
                group.getTitle(),
                group.getGroupSize(),
                group.getMaxMembers(),
                group.getStatus(),
                group.getGroupNote(),
                memberCount,
                group.getCreatedAt()
        );
    }

    public List<GroupResponse> toGroupResponseList(List<EventGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return Collections.emptyList();
        }

        return groups.stream()
                .map(this::toGroupResponse)
                .collect(Collectors.toList());
    }

}