package com.loopin.api.notifications.controller;

import com.loopin.api.notifications.enums.NotificationStatus;
import com.loopin.api.common.security.SecurityUtils;
import com.loopin.api.notifications.dto.response.NotificationResponse;
import com.loopin.api.users.entity.User;
import com.loopin.api.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @RequestParam(required = false) NotificationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(notificationService.getNotifications(currentUser(), status, pageable));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable UUID id) {
        return ResponseEntity.ok(notificationService.markAsRead(id, currentUser()));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead(currentUser());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(@PathVariable UUID id) {
        notificationService.archive(id, currentUser());
        return ResponseEntity.noContent().build();
    }

    private User currentUser() {
        return SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new com.loopin.api.common.exception.UnauthorizedException(
                        "Authentication is required"));
    }
}
