package com.loopin.api.events.service;

import com.loopin.api.events.dto.request.EventCreateRequest;
import com.loopin.api.events.dto.request.EventUpdateRequest;
import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.enums.EventCategory;
import com.loopin.api.events.enums.EventType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EventService {

    org.springframework.data.domain.Page<EventResponse> getPublishedEvents(
            EventType type,
            EventCategory category,
            String city,
            Boolean isFree,
            String search,
            LocalDate startDate,
            LocalDate endDate,
            org.springframework.data.domain.Pageable pageable
    );

    EventResponse updateEvent(UUID id, EventUpdateRequest request, String currentUsername);

    void deleteEvent(UUID id, String currentUsername);

    List<EventResponse> getRecommendedEvents(String currentUsername, int limit);
}
