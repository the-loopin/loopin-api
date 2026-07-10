package com.loopin.api.groups.removegroupmember;

import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.entity.GroupMember;
import com.loopin.api.groups.repository.EventGroupRepository;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.groups.shared.finder.GroupFinder;
import com.loopin.api.groups.shared.policy.GroupAdminPolicy;
import com.loopin.api.groups.shared.policy.GroupCapacityPolicy;
import com.loopin.api.groups.shared.policy.GroupMembershipPolicy;
import com.loopin.api.notifications.api.NotificationWriter;
import com.loopin.api.notifications.enums.NotificationReferenceType;
import com.loopin.api.notifications.enums.NotificationType;
import com.loopin.api.notifications.service.NotificationCommand;
import com.loopin.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RemoveGroupMemberHandler {
    private final EventGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final GroupFinder groupFinder;
    private final GroupAdminPolicy adminPolicy;
    private final GroupMembershipPolicy membershipPolicy;
    private final GroupCapacityPolicy capacityPolicy;
    private final NotificationWriter notificationWriter;

    @Transactional
    public void handle(RemoveGroupMemberCommand command) {
        EventGroup group = groupFinder.findGroup(command.groupId());
        adminPolicy.requireAdmin(group, command.currentUsername());
        membershipPolicy.requireMembershipChangesAllowed(group);
        User user = groupFinder.findActiveUser(command.userId());
        GroupMember member = memberRepository.findByGroupIdAndUserId(group.getId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));
        int memberCount = memberRepository.countByGroupId(group.getId());
        memberRepository.delete(member);
        if (capacityPolicy.refreshStatus(group, memberCount - 1)) {
            groupRepository.save(group);
        }
        notificationWriter.write(new NotificationCommand(user, NotificationType.GROUP_ACTIVITY,
                "Removed from group", "You were removed from \"" + group.getTitle() + "\".",
                NotificationReferenceType.GROUP, group.getPublicId()));
    }
}
