package com.loopin.api.notifications.dto.response;

import com.loopin.api.notifications.enums.NotificationReferenceType;
import com.loopin.api.notifications.enums.NotificationStatus;
import com.loopin.api.notifications.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String message,
        NotificationStatus status,
        NotificationReferenceType referenceType,
        UUID referenceId,
        LocalDateTime readAt,
        LocalDateTime archivedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
