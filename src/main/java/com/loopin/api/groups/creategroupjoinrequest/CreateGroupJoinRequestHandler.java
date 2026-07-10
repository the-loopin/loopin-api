package com.loopin.api.groups.creategroupjoinrequest;

import com.loopin.api.groups.dto.response.GroupJoinRequestResponse;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.entity.GroupJoinRequest;
import com.loopin.api.groups.enums.GroupStatus;
import com.loopin.api.groups.enums.RequestStatus;
import com.loopin.api.groups.repository.GroupJoinRequestRepository;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.groups.shared.joinrequest.GroupJoinRequestFinder;
import com.loopin.api.groups.shared.policy.GroupMembershipPolicy;
import com.loopin.api.moderation.ContentModerationService;
import com.loopin.api.notifications.api.NotificationWriter;
import com.loopin.api.notifications.enums.NotificationReferenceType;
import com.loopin.api.notifications.enums.NotificationType;
import com.loopin.api.notifications.service.NotificationCommand;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.api.UserLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class CreateGroupJoinRequestHandler {
    private final GroupJoinRequestFinder requestFinder;
    private final GroupJoinRequestRepository requestRepository;
    private final GroupMemberRepository memberRepository;
    private final UserLookup userLookup;
    private final GroupMembershipPolicy membershipPolicy;
    private final ContentModerationService moderationService;
    private final NotificationWriter notificationWriter;

    @Transactional
    public GroupJoinRequestResponse handle(CreateGroupJoinRequestCommand command) {
        EventGroup group = requestFinder.findGroup(command.groupId());
        User user = userLookup.findById(command.currentUserId());
        if (group.getStatus() != GroupStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Group is not open, join request cannot be sent");
        }
        membershipPolicy.requireNotMember(memberRepository.existsByGroupIdAndUserId(group.getId(), user.getId()));
        if (requestRepository.existsByGroupIdAndUserIdAndStatus(group.getId(), user.getId(), RequestStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A pending request already exists");
        }
        GroupJoinRequest request = new GroupJoinRequest();
        request.setGroup(group);
        request.setUser(user);
        request.setMessage(command.request().getMessage());
        boolean unsafeContent = !moderationService.moderate(command.request().getMessage()).isApproved();
        request.setStatus(unsafeContent ? RequestStatus.REJECTED : RequestStatus.PENDING);
        GroupJoinRequest saved = requestRepository.save(request);
        if (!unsafeContent) {
            notificationWriter.write(new NotificationCommand(group.getAdmin(), NotificationType.GROUP_ACTIVITY,
                    "New group join request", user.getName() + " requested to join \"" + group.getTitle() + "\".",
                    NotificationReferenceType.GROUP, group.getPublicId()));
        }
        return GroupJoinRequestResponse.from(saved);
    }
}
