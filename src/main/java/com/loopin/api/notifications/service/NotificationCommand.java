package com.loopin.api.notifications.service;

import com.loopin.api.notifications.enums.NotificationReferenceType;
import com.loopin.api.notifications.enums.NotificationType;
import com.loopin.api.core.users.entity.User;

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
