package com.loopin.api.groups.changegroupstatus;

import com.loopin.api.groups.dto.request.UpdateGroupStatusRequest;
import java.util.UUID;

public record ChangeGroupStatusCommand(UUID groupId, UpdateGroupStatusRequest request, String currentUsername) {
}
