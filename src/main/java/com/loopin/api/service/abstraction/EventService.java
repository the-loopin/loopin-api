package com.loopin.api.service.abstraction;

import com.loopin.api.dto.event.request.EventCreateRequest;
import com.loopin.api.dto.event.request.EventUpdateRequest;
import com.loopin.api.dto.event.response.EventResponse;
import com.loopin.api.common.enums.EventCategory;
import com.loopin.api.common.enums.EventType;

import java.time.LocalDate;
import java.util.List;

public interface EventService {

    List<EventResponse> getPublishedEvents(
            EventType type,
            EventCategory category,
            String city,
            Boolean isFree,
            String search,
            LocalDate startDate,
            LocalDate endDate
    );

    EventResponse getPublishedEventById(Long id);

    EventResponse createEvent(EventCreateRequest request, String currentUsername);

    EventResponse updateEvent(Long id, EventUpdateRequest request, String currentUsername);

    void deleteEvent(Long id, String currentUsername);
}
