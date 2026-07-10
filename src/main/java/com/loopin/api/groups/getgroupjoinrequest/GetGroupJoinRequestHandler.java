package com.loopin.api.groups.getgroupjoinrequest;

import com.loopin.api.groups.dto.response.GroupJoinRequestResponse;
import com.loopin.api.groups.entity.GroupJoinRequest;
import com.loopin.api.groups.shared.joinrequest.GroupJoinRequestFinder;
import com.loopin.api.groups.shared.policy.GroupJoinRequestAccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GetGroupJoinRequestHandler {
    private final GroupJoinRequestFinder requestFinder;
    private final GroupJoinRequestAccessPolicy accessPolicy;

    @Transactional(readOnly = true)
    public GroupJoinRequestResponse handle(GetGroupJoinRequestQuery query) {
        GroupJoinRequest request = requestFinder.findRequest(query.groupId(), query.requestId());
        accessPolicy.requireAdminOrRequester(request, query.currentUserId());
        return GroupJoinRequestResponse.from(request);
    }
}
