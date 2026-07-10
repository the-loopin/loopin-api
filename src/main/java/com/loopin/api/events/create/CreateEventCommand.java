package com.loopin.api.events.create;

import com.loopin.api.events.dto.request.EventCreateRequest;

public record CreateEventCommand(EventCreateRequest request, String currentUsername) {
}
