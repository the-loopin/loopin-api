package com.loopin.api.core.events.controller;

import com.loopin.api.core.events.dto.request.EventCreateRequest;
import com.loopin.api.core.events.dto.request.EventUpdateRequest;
import com.loopin.api.core.events.dto.response.EventResponse;
import com.loopin.api.core.events.enums.EventCategory;
import com.loopin.api.core.events.enums.EventType;
import com.loopin.api.core.events.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.loopin.api.common.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<Page<EventResponse>> getPublishedEvents(
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) EventCategory category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean isFree,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable
    ) {
        Page<EventResponse> events = eventService.getPublishedEvents(
                type,
                category,
                city,
                isFree,
                search,
                startDate,
                endDate,
                pageable
        );

        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getPublishedEventById(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getPublishedEventById(id));
    }

    @GetMapping("/recommended")
    public ResponseEntity<List<EventResponse>> getRecommendedEvents(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<EventResponse> events = eventService.getRecommendedEvents(SecurityUtils.getRequiredCurrentUserEmail(), limit);
        return ResponseEntity.ok(events);
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody EventCreateRequest request
    ) {
        EventResponse createdEvent = eventService.createEvent(request, SecurityUtils.getRequiredCurrentUserEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEvent);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody EventUpdateRequest request
    ) {
        return ResponseEntity.ok(eventService.updateEvent(id, request, SecurityUtils.getRequiredCurrentUserEmail()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable UUID id
    ) {
        eventService.deleteEvent(id, SecurityUtils.getRequiredCurrentUserEmail());
        return ResponseEntity.noContent().build();
    }
}
