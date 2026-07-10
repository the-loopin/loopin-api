package com.loopin.api.groups.addgroupmember;

import com.loopin.api.groups.dto.response.GroupMemberResponse;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.entity.GroupMember;
import com.loopin.api.groups.mapper.GroupMemberMapper;
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
public class AddGroupMemberHandler {
    private final EventGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final GroupMemberMapper memberMapper;
    private final GroupFinder groupFinder;
    private final GroupAdminPolicy adminPolicy;
    private final GroupMembershipPolicy membershipPolicy;
    private final GroupCapacityPolicy capacityPolicy;
    private final NotificationWriter notificationWriter;

    @Transactional
    public GroupMemberResponse handle(AddGroupMemberCommand command) {
        EventGroup group = groupFinder.findGroup(command.groupId());
        adminPolicy.requireAdmin(group, command.currentUsername());
        capacityPolicy.applyMaximumFromSize(group);
        int memberCount = memberRepository.countByGroupId(group.getId());
        if (capacityPolicy.refreshStatus(group, memberCount)) {
            groupRepository.save(group);
        }
        membershipPolicy.requireCanAddMember(group, memberCount);
        User user = groupFinder.findActiveUser(command.userId());
        membershipPolicy.requireNotMember(memberRepository.existsByGroupIdAndUserId(group.getId(), user.getId()));
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);
        GroupMember savedMember = memberRepository.save(member);
        if (capacityPolicy.refreshStatus(group, memberCount + 1)) {
            groupRepository.save(group);
        }
        notificationWriter.write(new NotificationCommand(user, NotificationType.GROUP_INVITATION,
                "Added to group", "You were added to \"" + group.getTitle() + "\".",
                NotificationReferenceType.GROUP, group.getPublicId()));
        return memberMapper.toResponse(savedMember);
    }
}
