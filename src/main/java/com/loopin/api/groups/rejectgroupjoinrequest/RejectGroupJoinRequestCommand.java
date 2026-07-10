package com.loopin.api.groups.rejectgroupjoinrequest;

import java.util.UUID;
public record RejectGroupJoinRequestCommand(UUID groupId, UUID requestId, Long currentUserId) {}
