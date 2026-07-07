package com.loopin.api.service.abstraction;

import com.loopin.api.dto.event.request.EventCreateRequest;
import com.loopin.api.dto.event.request.EventUpdateRequest;
import com.loopin.api.dto.event.response.EventResponse;
import com.loopin.api.common.enums.EventCategory;
import com.loopin.api.common.enums.EventType;

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

    EventResponse getPublishedEventById(UUID id);

    EventResponse createEvent(EventCreateRequest request, String currentUsername);

    EventResponse updateEvent(UUID id, EventUpdateRequest request, String currentUsername);

    void deleteEvent(UUID id, String currentUsername);

    List<EventResponse> getRecommendedEvents(String currentUsername, int limit);
}
