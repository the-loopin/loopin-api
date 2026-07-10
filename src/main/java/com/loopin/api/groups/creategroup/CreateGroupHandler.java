package com.loopin.api.groups.creategroup;

import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.groups.dto.response.GroupResponse;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.entity.GroupMember;
import com.loopin.api.groups.mapper.GroupMapper;
import com.loopin.api.groups.repository.EventGroupRepository;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.groups.shared.finder.GroupFinder;
import com.loopin.api.groups.shared.policy.GroupCapacityPolicy;
import com.loopin.api.moderation.ContentModerationService;
import com.loopin.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CreateGroupHandler {
    private final EventGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final EventRepository eventRepository;
    private final GroupMapper groupMapper;
    private final GroupFinder groupFinder;
    private final GroupCapacityPolicy capacityPolicy;
    private final ContentModerationService moderationService;

    @Transactional
    public GroupResponse handle(CreateGroupCommand command) {
        var request = command.request();
        if (!moderationService.moderate(request.getTitle(), request.getGroupNote()).isApproved()) {
            throw new IllegalArgumentException("Content contains blocked language and cannot be published");
        }
        User creator = groupFinder.findCurrentUser(command.currentUsername());
        Event event = eventRepository.findByPublicIdAndDeletedAtIsNull(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + request.getEventId()));
        EventGroup group = groupMapper.toEntity(request, creator, event);
        capacityPolicy.applyMaximumFromSize(group);
        EventGroup savedGroup = groupRepository.save(group);
        GroupMember creatorMembership = new GroupMember();
        creatorMembership.setGroup(savedGroup);
        creatorMembership.setUser(creator);
        memberRepository.save(creatorMembership);
        capacityPolicy.refreshStatus(savedGroup, 1);
        return groupMapper.toGroupResponse(savedGroup);
    }
}
