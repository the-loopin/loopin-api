package com.loopin.api.events.service;

import com.loopin.api.events.dto.response.EventResponse;

import java.util.List;
import java.util.UUID;

public interface UserLoopedEventService {

    EventResponse loopIn(UUID eventId, String currentUsername);

    void removeLoopIn(UUID eventId, String currentUsername);

    List<EventResponse> getMyLoopedEvents(String currentUsername);
}
