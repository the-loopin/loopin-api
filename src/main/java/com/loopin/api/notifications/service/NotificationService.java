package com.loopin.api.notifications.service;

import com.loopin.api.notifications.enums.NotificationStatus;
import com.loopin.api.notifications.dto.response.NotificationResponse;
import com.loopin.api.notifications.entity.Notification;
import com.loopin.api.core.users.entity.User;
import com.loopin.api.notifications.service.NotificationCommand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface NotificationService {

    Notification create(NotificationCommand command);

    List<Notification> createAll(Collection<NotificationCommand> commands);

    Page<NotificationResponse> getNotifications(User currentUser, NotificationStatus status, Pageable pageable);

    NotificationResponse markAsRead(UUID notificationId, User currentUser);

    int markAllAsRead(User currentUser);

    void archive(UUID notificationId, User currentUser);

    int broadcastAnnouncement(String title, String message);
}
