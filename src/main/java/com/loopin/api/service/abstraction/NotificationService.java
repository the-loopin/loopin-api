package com.loopin.api.service.abstraction;

import com.loopin.api.common.enums.NotificationStatus;
import com.loopin.api.dto.notification.response.NotificationResponse;
import com.loopin.api.entity.Notification;
import com.loopin.api.entity.User;
import com.loopin.api.service.notification.NotificationCommand;
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
