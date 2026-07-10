package com.loopin.api.groups.approvegroupjoinrequest;

import com.loopin.api.groups.dto.response.GroupJoinRequestResponse;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.entity.GroupJoinRequest;
import com.loopin.api.groups.entity.GroupMember;
import com.loopin.api.groups.enums.RequestStatus;
import com.loopin.api.groups.repository.EventGroupRepository;
import com.loopin.api.groups.repository.GroupJoinRequestRepository;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.groups.shared.joinrequest.GroupJoinRequestFinder;
import com.loopin.api.groups.shared.policy.GroupAdminPolicy;
import com.loopin.api.groups.shared.policy.GroupCapacityPolicy;
import com.loopin.api.groups.shared.policy.GroupMembershipPolicy;
import com.loopin.api.notifications.enums.NotificationReferenceType;
import com.loopin.api.notifications.enums.NotificationType;
import com.loopin.api.notifications.service.NotificationCommand;
import com.loopin.api.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class ApproveGroupJoinRequestHandler {
    private final GroupJoinRequestFinder requestFinder;
    private final GroupJoinRequestRepository requestRepository;
    private final GroupMemberRepository memberRepository;
    private final EventGroupRepository groupRepository;
    private final GroupAdminPolicy adminPolicy;
    private final GroupMembershipPolicy membershipPolicy;
    private final GroupCapacityPolicy capacityPolicy;
    private final NotificationService notificationService;

    @Transactional
    public GroupJoinRequestResponse handle(ApproveGroupJoinRequestCommand command) {
        GroupJoinRequest request = requestFinder.findRequest(command.groupId(), command.requestId());
        EventGroup group = request.getGroup();
        adminPolicy.requireAdmin(group, command.currentUserId());
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request has already been processed");
        }
        int memberCount = memberRepository.countByGroupId(group.getId());
        membershipPolicy.requireCanAddMember(group, memberCount);
        membershipPolicy.requireNotMember(memberRepository.existsByGroupIdAndUserId(group.getId(), request.getUser().getId()));
        request.setStatus(RequestStatus.ACCEPTED);
        requestRepository.save(request);
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(request.getUser());
        memberRepository.save(member);
        if (capacityPolicy.refreshStatus(group, memberCount + 1)) groupRepository.save(group);
        notificationService.create(new NotificationCommand(request.getUser(), NotificationType.GROUP_INVITATION,
                "Join request approved", "Your request to join \"" + group.getTitle() + "\" was approved.",
                NotificationReferenceType.GROUP, group.getPublicId()));
        return GroupJoinRequestResponse.from(request);
    }
}
