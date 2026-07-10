package com.loopin.api.groups.updategroup;

import com.loopin.api.groups.dto.request.UpdateGroupRequest;
import java.util.UUID;

public record UpdateGroupCommand(UUID groupId, UpdateGroupRequest request, String currentUsername) {
}
