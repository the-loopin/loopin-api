package com.loopin.api.groups.approvegroupjoinrequest;

import java.util.UUID;

public record ApproveGroupJoinRequestCommand(UUID groupId, UUID requestId, Long currentUserId) {
}
