package com.loopin.api.events.removeloopin;

import java.util.UUID;

public record RemoveLoopInCommand(
    UUID eventId,
    String currentUsername
) {
}
