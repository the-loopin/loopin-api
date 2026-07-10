package com.loopin.api.events.update;

import com.loopin.api.events.dto.request.EventUpdateRequest;

import java.util.UUID;

public record UpdateEventCommand(UUID id, EventUpdateRequest request, String currentUsername) {
}
