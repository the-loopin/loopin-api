package com.loopin.api.events.controller;

import com.loopin.api.events.dto.request.EventCreateRequest;
import com.loopin.api.events.dto.request.EventUpdateRequest;
import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.create.CreateEventCommand;
import com.loopin.api.events.create.CreateEventHandler;
import com.loopin.api.events.enums.EventCategory;
import com.loopin.api.events.enums.EventType;
import com.loopin.api.events.getpublishedbyid.GetPublishedEventByIdHandler;
import com.loopin.api.events.getpublishedbyid.GetPublishedEventByIdQuery;
import com.loopin.api.events.getrecommendedevents.GetRecommendedEventsHandler;
import com.loopin.api.events.getrecommendedevents.GetRecommendedEventsQuery;
import com.loopin.api.events.listpublishedevents.ListPublishedEventsHandler;
import com.loopin.api.events.listpublishedevents.ListPublishedEventsQuery;
import com.loopin.api.events.service.EventService;
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
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springdoc.core.annotations.ParameterObject;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/events")
@RequiredArgsConstructor
@Tag(name = "Event Management", description = "Endpoints for managing and querying events")
public class EventController {

    private final EventService eventService;
    private final CreateEventHandler createEventHandler;
    private final GetPublishedEventByIdHandler getPublishedEventByIdHandler;
    private final ListPublishedEventsHandler listPublishedEventsHandler;
    private final GetRecommendedEventsHandler getRecommendedEventsHandler;

    @GetMapping
    @Operation(summary = "Get published events", description = "Retrieve a paginated list of published events based on filter criteria.")
    public ResponseEntity<Page<EventResponse>> getPublishedEvents(
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) EventCategory category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean isFree,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @ParameterObject Pageable pageable
    ) {
        Page<EventResponse> events = listPublishedEventsHandler.handle(new ListPublishedEventsQuery(
                type,
                category,
                city,
                isFree,
                search,
                startDate,
                endDate,
                pageable
        ));

        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getPublishedEventById(@PathVariable UUID id) {
        return ResponseEntity.ok(getPublishedEventByIdHandler.handle(new GetPublishedEventByIdQuery(id)));
    }

    @GetMapping("/recommended")
    @Operation(summary = "Get recommended events", description = "Retrieve event recommendations. Requires authentication.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<List<EventResponse>> getRecommendedEvents(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<EventResponse> events = getRecommendedEventsHandler.handle(
                new GetRecommendedEventsQuery(SecurityUtils.getRequiredCurrentUserEmail(), limit)
        );
        return ResponseEntity.ok(events);
    }

    @PostMapping
    @Operation(summary = "Create a new event", description = "Create a new event. Requires authentication.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody EventCreateRequest request
    ) {
        EventResponse createdEvent = createEventHandler.handle(
                new CreateEventCommand(request, SecurityUtils.getRequiredCurrentUserEmail())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEvent);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an event", description = "Update an event. Requires authentication.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody EventUpdateRequest request
    ) {
        return ResponseEntity.ok(eventService.updateEvent(id, request, SecurityUtils.getRequiredCurrentUserEmail()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an event", description = "Delete an event. Requires authentication.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable UUID id
    ) {
        eventService.deleteEvent(id, SecurityUtils.getRequiredCurrentUserEmail());
        return ResponseEntity.noContent().build();
    }
}
