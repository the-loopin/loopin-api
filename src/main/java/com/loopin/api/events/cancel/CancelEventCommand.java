package com.loopin.api.events.cancel;

import java.util.UUID;

public record CancelEventCommand(UUID id, String currentUsername) {
}
