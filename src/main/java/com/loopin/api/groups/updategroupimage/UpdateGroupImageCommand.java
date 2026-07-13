package com.loopin.api.groups.updategroupimage;

import java.util.UUID;

public record UpdateGroupImageCommand(
    UUID groupId,
    UUID mediaId,
    String currentUsername
) {
}
