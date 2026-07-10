package com.loopin.api.groups.shared.policy;

import com.loopin.api.common.exception.ForbiddenAccessException;
import com.loopin.api.groups.entity.GroupJoinRequest;
import org.springframework.stereotype.Component;

@Component
public class GroupJoinRequestAccessPolicy {
    public void requireAdminOrRequester(GroupJoinRequest request, Long userId) {
        boolean isAdmin = request.getGroup().getAdmin() != null
                && request.getGroup().getAdmin().getId().equals(userId);
        boolean isRequester = request.getUser().getId().equals(userId);
        if (!isAdmin && !isRequester) {
            throw new ForbiddenAccessException("You cannot access this join request");
        }
    }
}
