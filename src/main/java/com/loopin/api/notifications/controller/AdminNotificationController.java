package com.loopin.api.notifications.controller;

import com.loopin.api.notifications.dto.request.CreateAnnouncementRequest;
import com.loopin.api.notifications.dto.response.AnnouncementResponse;
import com.loopin.api.notifications.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;

    @PostMapping("/announcements")
    public ResponseEntity<AnnouncementResponse> createAnnouncement(
            @Valid @RequestBody CreateAnnouncementRequest request) {
        int count = notificationService.broadcastAnnouncement(request.title(), request.message());
        return ResponseEntity.status(HttpStatus.CREATED).body(new AnnouncementResponse(count));
    }
}
