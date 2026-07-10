package com.loopin.api.groups.deletegroupjoinrequest;

import com.loopin.api.groups.entity.GroupJoinRequest;
import com.loopin.api.groups.repository.GroupJoinRequestRepository;
import com.loopin.api.groups.shared.joinrequest.GroupJoinRequestFinder;
import com.loopin.api.groups.shared.policy.GroupJoinRequestAccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DeleteGroupJoinRequestHandler {
    private final GroupJoinRequestFinder requestFinder;
    private final GroupJoinRequestRepository requestRepository;
    private final GroupJoinRequestAccessPolicy accessPolicy;

    @Transactional
    public void handle(DeleteGroupJoinRequestCommand command) {
        GroupJoinRequest request = requestFinder.findRequest(command.groupId(), command.requestId());
        accessPolicy.requireAdminOrRequester(request, command.currentUserId());
        requestRepository.delete(request);
    }
}
