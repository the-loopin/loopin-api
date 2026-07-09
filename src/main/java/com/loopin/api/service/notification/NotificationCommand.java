package com.loopin.api.service.notification;

import com.loopin.api.common.enums.NotificationReferenceType;
import com.loopin.api.common.enums.NotificationType;
import com.loopin.api.entity.User;

import java.util.UUID;

public record NotificationCommand(
        User recipient,
        NotificationType type,
        String title,
        String message,
        NotificationReferenceType referenceType,
        UUID referenceId,
        String deduplicationKey) {

    public NotificationCommand(
            User recipient,
            NotificationType type,
            String title,
            String message,
            NotificationReferenceType referenceType,
            UUID referenceId) {
        this(recipient, type, title, message, referenceType, referenceId, null);
    }
}
