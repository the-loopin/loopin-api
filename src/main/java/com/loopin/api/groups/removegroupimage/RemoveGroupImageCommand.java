package com.loopin.api.groups.removegroupimage;

import java.util.UUID;

public record RemoveGroupImageCommand(
    UUID groupId,
    String currentUsername
) {
}
