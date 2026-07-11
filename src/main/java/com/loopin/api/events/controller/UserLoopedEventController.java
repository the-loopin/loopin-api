package com.loopin.api.events.controller;

import com.loopin.api.common.security.SecurityUtils;
import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.service.UserLoopedEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class UserLoopedEventController {

    private final UserLoopedEventService userLoopedEventService;

    @PostMapping("/events/{eventId}/loopin")
    public ResponseEntity<EventResponse> loopIn(@PathVariable UUID eventId) {
        return ResponseEntity.ok(userLoopedEventService.loopIn(
                eventId,
                SecurityUtils.getRequiredCurrentUserEmail()));
    }

    @DeleteMapping("/events/{eventId}/loopin")
    public ResponseEntity<Void> removeLoopIn(@PathVariable UUID eventId) {
        userLoopedEventService.removeLoopIn(
                eventId,
                SecurityUtils.getRequiredCurrentUserEmail());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/looped-events")
    public ResponseEntity<List<EventResponse>> getMyLoopedEvents() {
        return ResponseEntity.ok(userLoopedEventService.getMyLoopedEvents(
                SecurityUtils.getRequiredCurrentUserEmail()));
    }
}
