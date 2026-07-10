package com.loopin.api.groups.rejectgroupjoinrequest;

import com.loopin.api.groups.dto.response.GroupJoinRequestResponse;
import com.loopin.api.groups.entity.GroupJoinRequest;
import com.loopin.api.groups.enums.RequestStatus;
import com.loopin.api.groups.repository.GroupJoinRequestRepository;
import com.loopin.api.groups.shared.joinrequest.GroupJoinRequestFinder;
import com.loopin.api.groups.shared.policy.GroupAdminPolicy;
import com.loopin.api.notifications.api.NotificationWriter;
import com.loopin.api.notifications.enums.NotificationReferenceType;
import com.loopin.api.notifications.enums.NotificationType;
import com.loopin.api.notifications.service.NotificationCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class RejectGroupJoinRequestHandler {
    private final GroupJoinRequestFinder requestFinder;
    private final GroupJoinRequestRepository requestRepository;
    private final GroupAdminPolicy adminPolicy;
    private final NotificationWriter notificationWriter;

    @Transactional
    public GroupJoinRequestResponse handle(RejectGroupJoinRequestCommand command) {
        GroupJoinRequest request = requestFinder.findRequest(command.groupId(), command.requestId());
        adminPolicy.requireAdmin(request.getGroup(), command.currentUserId());
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request has already been processed");
        }
        request.setStatus(RequestStatus.REJECTED);
        GroupJoinRequest saved = requestRepository.save(request);
        notificationWriter.write(new NotificationCommand(request.getUser(), NotificationType.GROUP_ACTIVITY,
                "Join request declined", "Your request to join \"" + request.getGroup().getTitle() + "\" was declined.",
                NotificationReferenceType.GROUP, request.getGroup().getPublicId()));
        return GroupJoinRequestResponse.from(saved);
    }
}
