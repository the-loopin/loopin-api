package com.loopin.api.groups.addgroupmember;

import java.util.UUID;

public record AddGroupMemberCommand(UUID groupId, UUID userId, String currentUsername) {
}
