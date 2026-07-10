package com.loopin.api.groups.creategroupjoinrequest;

import com.loopin.api.groups.dto.request.CreateGroupJoinRequestRequest;
import java.util.UUID;

public record CreateGroupJoinRequestCommand(UUID groupId, Long currentUserId,
                                            CreateGroupJoinRequestRequest request) {
}
