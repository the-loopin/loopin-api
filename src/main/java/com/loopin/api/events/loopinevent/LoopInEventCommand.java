package com.loopin.api.events.loopinevent;

import java.util.UUID;

public record LoopInEventCommand(
    UUID eventId,
    String currentUsername
) {
}
