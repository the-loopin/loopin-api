package com.loopin.api.groups.removegroupmember;

import java.util.UUID;

public record RemoveGroupMemberCommand(UUID groupId, UUID userId, String currentUsername) {
}
