package com.loopin.api.notifications.repository;

import com.loopin.api.notifications.enums.NotificationStatus;
import com.loopin.api.notifications.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientIdAndStatusNot(
            Long recipientId, NotificationStatus status, Pageable pageable);

    Page<Notification> findByRecipientIdAndStatus(
            Long recipientId, NotificationStatus status, Pageable pageable);

    Optional<Notification> findByPublicIdAndRecipientId(UUID publicId, Long recipientId);

    boolean existsByDeduplicationKey(String deduplicationKey);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Notification notification
               set notification.status = com.loopin.api.notifications.enums.NotificationStatus.READ,
                   notification.readAt = :readAt
             where notification.recipient.id = :recipientId
               and notification.status = com.loopin.api.notifications.enums.NotificationStatus.UNREAD
            """)
    int markAllUnreadAsRead(
            @Param("recipientId") Long recipientId,
            @Param("readAt") LocalDateTime readAt);
}
