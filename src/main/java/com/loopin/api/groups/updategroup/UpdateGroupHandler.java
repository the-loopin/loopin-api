package com.loopin.api.groups.updategroup;

import com.loopin.api.groups.dto.response.GroupResponse;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.mapper.GroupMapper;
import com.loopin.api.groups.repository.EventGroupRepository;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.groups.shared.finder.GroupFinder;
import com.loopin.api.groups.shared.policy.GroupAdminPolicy;
import com.loopin.api.groups.shared.policy.GroupCapacityPolicy;
import com.loopin.api.groups.shared.policy.GroupMembershipPolicy;
import com.loopin.api.moderation.ContentModerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UpdateGroupHandler {
    private final EventGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final GroupMapper groupMapper;
    private final GroupFinder groupFinder;
    private final GroupAdminPolicy adminPolicy;
    private final GroupMembershipPolicy membershipPolicy;
    private final GroupCapacityPolicy capacityPolicy;
    private final ContentModerationService moderationService;

    @Transactional
    public GroupResponse handle(UpdateGroupCommand command) {
        EventGroup group = groupFinder.findGroup(command.groupId());
        adminPolicy.requireAdmin(group, command.currentUsername());
        membershipPolicy.requireMembershipChangesAllowed(group);
        var request = command.request();
        if (!moderationService.moderate(request.getTitle(), request.getGroupNote()).isApproved()) {
            throw new IllegalArgumentException("Content contains blocked language and cannot be published");
        }
        int memberCount = memberRepository.countByGroupId(group.getId());
        if (request.getTitle() != null) group.setTitle(request.getTitle());
        if (request.getGroupSize() != null) group.setGroupSize(request.getGroupSize());
        capacityPolicy.applyMaximumFromSize(group);
        capacityPolicy.requireCapacityNotBelowMemberCount(group, memberCount);
        if (request.getGroupNote() != null) group.setGroupNote(request.getGroupNote());
        capacityPolicy.refreshStatus(group, memberCount);
        return groupMapper.toGroupResponse(groupRepository.save(group));
    }
}
