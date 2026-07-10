package com.loopin.api.events.service;

import com.loopin.api.events.dto.request.EventUpdateRequest;
import com.loopin.api.events.dto.response.EventResponse;

import java.util.UUID;

public interface EventService {

    EventResponse updateEvent(UUID id, EventUpdateRequest request, String currentUsername);

    void deleteEvent(UUID id, String currentUsername);
}
