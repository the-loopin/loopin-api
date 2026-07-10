package com.loopin.api.groups.listgroupjoinrequests;

import java.util.UUID;
public record ListGroupJoinRequestsQuery(UUID groupId, Long currentUserId) {}
