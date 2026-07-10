package com.loopin.api.groups.creategroup;

import com.loopin.api.groups.dto.request.CreateGroupRequest;

public record CreateGroupCommand(CreateGroupRequest request, String currentUsername) {
}
