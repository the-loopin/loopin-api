package com.loopin.api.mapper;

import com.loopin.api.dto.event.request.EventCreateRequest;
import com.loopin.api.dto.event.request.EventUpdateRequest;
import com.loopin.api.dto.event.response.EventResponse;
import com.loopin.api.entity.Event;
import com.loopin.api.common.enums.EventStatus;
import com.loopin.api.entity.EventInterest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EventMapper {

    private final InterestMapper interestMapper;

    public Event toEntity(EventCreateRequest request) {
        Event event = new Event();

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setType(request.getType());
        event.setCategory(request.getCategory());
        event.setCity(request.getCity());
        event.setAddress(request.getAddress());
        event.setStartDateTime(request.getStartDateTime());
        event.setEndDateTime(request.getEndDateTime());
        event.setIsFree(request.getIsFree());
        event.setPrice(request.getPrice());
        event.setOrganizerName(request.getOrganizerName());
        event.setImageUrl(request.getImageUrl());
        event.setStatus(request.getStatus() != null ? request.getStatus() : EventStatus.PUBLISHED);

        return event;
    }

    public void updateEntity(Event event, EventUpdateRequest request) {
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setType(request.getType());
        event.setCategory(request.getCategory());
        event.setCity(request.getCity());
        event.setAddress(request.getAddress());
        event.setStartDateTime(request.getStartDateTime());
        event.setEndDateTime(request.getEndDateTime());
        event.setIsFree(request.getIsFree());
        event.setPrice(request.getPrice());
        event.setOrganizerName(request.getOrganizerName());
        event.setImageUrl(request.getImageUrl());
        event.setStatus(request.getStatus());
    }

    public EventResponse toResponse(Event event) {
        return new EventResponse(
                event.getPublicId(),
                event.getTitle(),
                event.getDescription(),
                event.getType(),
                event.getCategory(),
                event.getCity(),
                event.getAddress(),
                event.getStartDateTime(),
                event.getEndDateTime(),
                event.getIsFree(),
                event.getPrice(),
                event.getOrganizerName(),
                event.getImageUrl(),
                event.getStatus(),
                mapInterests(event),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    private List<com.loopin.api.dto.interest.InterestResponse> mapInterests(Event event) {
        if (event.getInterests() == null) {
            return List.of();
        }

        return event.getInterests()
                .stream()
                .map(EventInterest::getInterest)
                .sorted(Comparator.comparing(interest -> interest.getName().toLowerCase()))
                .map(interestMapper::toResponse)
                .toList();
    }
}
