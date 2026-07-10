package com.loopin.api.groups.getgroupjoinrequest;

import java.util.UUID;
public record GetGroupJoinRequestQuery(UUID groupId, UUID requestId, Long currentUserId) {}
