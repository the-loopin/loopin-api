package com.loopin.api.notifications.service;

import com.loopin.api.notifications.enums.NotificationChannel;
import com.loopin.api.common.metrics.LoopinOperation;
import com.loopin.api.common.metrics.LoopinMetrics;
import com.loopin.api.notifications.enums.NotificationDeliveryStatus;
import com.loopin.api.notifications.enums.NotificationReferenceType;
import com.loopin.api.notifications.enums.NotificationStatus;
import com.loopin.api.notifications.enums.NotificationType;
import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.notifications.dto.response.NotificationResponse;
import com.loopin.api.notifications.entity.Notification;
import com.loopin.api.notifications.entity.NotificationDelivery;
import com.loopin.api.users.entity.User;
import com.loopin.api.notifications.repository.NotificationDeliveryRepository;
import com.loopin.api.notifications.repository.NotificationRepository;
import com.loopin.api.users.repository.UserRepository;
import com.loopin.api.notifications.service.NotificationService;
import com.loopin.api.notifications.service.NotificationCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final UserRepository userRepository;
    private final LoopinMetrics loopinMetrics;

    @Override
    @Transactional
    @LoopinOperation(domain = "notifications", operation = "create")
    public Notification create(NotificationCommand command) {
        if (command.deduplicationKey() != null
                && notificationRepository.existsByDeduplicationKey(command.deduplicationKey())) {
            return null;
        }

        Notification notification = new Notification();
        notification.setRecipient(command.recipient());
        notification.setType(command.type());
        notification.setTitle(command.title());
        notification.setMessage(command.message());
        notification.setReferenceType(command.referenceType());
        notification.setReferenceId(command.referenceId());
        notification.setDeduplicationKey(command.deduplicationKey());

        Notification saved = notificationRepository.saveAndFlush(notification);
        queueN8nDelivery(saved);
        return saved;
    }

    @Override
    @Transactional
    public List<Notification> createAll(Collection<NotificationCommand> commands) {
        long startNanos = System.nanoTime();
        List<Notification> created = new ArrayList<>();
        try {
            for (NotificationCommand command : commands) {
                Notification notification = create(command);
                if (notification != null) {
                    created.add(notification);
                }
            }
        } catch (RuntimeException exception) {
            loopinMetrics.recordOperation(
                "notifications", "create", false,
                java.time.Duration.ofNanos(System.nanoTime() - startNanos)
            );
            throw exception;
        }

        // create(...) is a self-invocation here, so its AOP metric is bypassed. Count each
        // persisted notification explicitly, while preserving the same bounded metric labels.
        loopinMetrics.recordOperations(
            "notifications", "create", true, created.size(),
            java.time.Duration.ofNanos(System.nanoTime() - startNanos)
        );
        return created;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(
            User currentUser,
            NotificationStatus status,
            Pageable pageable) {
        Page<Notification> notifications = status == null
                ? notificationRepository.findByRecipientIdAndStatusNot(
                        currentUser.getId(), NotificationStatus.ARCHIVED, pageable)
                : notificationRepository.findByRecipientIdAndStatus(
                        currentUser.getId(), status, pageable);
        return notifications.map(this::toResponse);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, User currentUser) {
        Notification notification = findOwned(notificationId, currentUser.getId());
        if (notification.getStatus() == NotificationStatus.ARCHIVED) {
            throw new ResourceNotFoundException("Notification not found: " + notificationId);
        }
        if (notification.getStatus() == NotificationStatus.UNREAD) {
            notification.setStatus(NotificationStatus.READ);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
        return toResponse(notification);
    }

    @Override
    @Transactional
    public int markAllAsRead(User currentUser) {
        return notificationRepository.markAllUnreadAsRead(currentUser.getId(), LocalDateTime.now());
    }

    @Override
    @Transactional
    public void archive(UUID notificationId, User currentUser) {
        Notification notification = findOwned(notificationId, currentUser.getId());
        if (notification.getStatus() != NotificationStatus.ARCHIVED) {
            notification.setStatus(NotificationStatus.ARCHIVED);
            notification.setArchivedAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public int broadcastAnnouncement(String title, String message) {
        List<User> recipients = userRepository.findAllByIsActiveTrueAndDeletedAtIsNull();
        List<NotificationCommand> commands = recipients.stream()
                .map(user -> new NotificationCommand(
                        user,
                        NotificationType.SYSTEM_ANNOUNCEMENT,
                        title,
                        message,
                        NotificationReferenceType.SYSTEM,
                        null))
                .toList();
        return createAll(commands).size();
    }

    private Notification findOwned(UUID notificationId, Long recipientId) {
        return notificationRepository.findByPublicIdAndRecipientId(notificationId, recipientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found: " + notificationId));
    }

    private void queueN8nDelivery(Notification notification) {
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setNotification(notification);
        delivery.setChannel(NotificationChannel.N8N);
        delivery.setStatus(NotificationDeliveryStatus.PENDING);
        delivery.setNextAttemptAt(LocalDateTime.now());
        deliveryRepository.save(delivery);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getPublicId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getStatus(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.getReadAt(),
                notification.getArchivedAt(),
                notification.getCreatedAt(),
                notification.getUpdatedAt());
    }
}
