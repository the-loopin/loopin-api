package com.loopin.api.events.delete;

import java.util.UUID;

public record DeleteEventCommand(UUID id, String currentUsername) {
}
