package com.loopin.api.controller;

import com.loopin.api.dto.event.request.EventCreateRequest;
import com.loopin.api.dto.event.request.EventUpdateRequest;
import com.loopin.api.dto.event.response.EventResponse;
import com.loopin.api.common.enums.EventCategory;
import com.loopin.api.common.enums.EventType;
import com.loopin.api.service.abstraction.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventResponse>> getPublishedEvents(
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) EventCategory category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean isFree,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<EventResponse> events = eventService.getPublishedEvents(
                type,
                category,
                city,
                isFree,
                search,
                startDate,
                endDate
        );

        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getPublishedEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getPublishedEventById(id));
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventCreateRequest request) {
        EventResponse createdEvent = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEvent);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventUpdateRequest request
    ) {
        return ResponseEntity.ok(eventService.updateEvent(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}