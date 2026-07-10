package com.loopin.api.groups.deletegroupjoinrequest;

import java.util.UUID;
public record DeleteGroupJoinRequestCommand(UUID groupId, UUID requestId, Long currentUserId) {}
