package com.loopin.api.service.implementation;

import com.loopin.api.common.enums.NotificationReferenceType;
import com.loopin.api.common.enums.NotificationStatus;
import com.loopin.api.common.enums.NotificationType;
import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.entity.Notification;
import com.loopin.api.entity.NotificationDelivery;
import com.loopin.api.entity.User;
import com.loopin.api.repository.NotificationDeliveryRepository;
import com.loopin.api.repository.NotificationRepository;
import com.loopin.api.repository.UserRepository;
import com.loopin.api.service.notification.NotificationCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceImplTest {

    private NotificationRepository notificationRepository;
    private NotificationDeliveryRepository deliveryRepository;
    private UserRepository userRepository;
    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        deliveryRepository = mock(NotificationDeliveryRepository.class);
        userRepository = mock(UserRepository.class);
        service = new NotificationServiceImpl(
                notificationRepository, deliveryRepository, userRepository);
    }

    @Test
    void create_PersistsNotificationAndOutboxTogether() {
        User recipient = user(10L);
        Notification saved = notification(recipient, NotificationStatus.UNREAD);
        when(notificationRepository.saveAndFlush(any(Notification.class))).thenReturn(saved);

        Notification result = service.create(new NotificationCommand(
                recipient,
                NotificationType.EVENT_UPDATE,
                "Event updated",
                "Details changed",
                NotificationReferenceType.EVENT,
                UUID.randomUUID()));

        assertEquals(saved, result);
        ArgumentCaptor<NotificationDelivery> deliveryCaptor =
                ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(deliveryRepository).save(deliveryCaptor.capture());
        assertEquals(saved, deliveryCaptor.getValue().getNotification());
        assertNotNull(deliveryCaptor.getValue().getNextAttemptAt());
    }

    @Test
    void create_WithExistingDeduplicationKey_DoesNothing() {
        when(notificationRepository.existsByDeduplicationKey("reminder-key")).thenReturn(true);

        Notification result = service.create(new NotificationCommand(
                user(10L),
                NotificationType.EVENT_REMINDER,
                "Reminder",
                "Starts soon",
                NotificationReferenceType.EVENT,
                UUID.randomUUID(),
                "reminder-key"));

        assertEquals(null, result);
        verify(notificationRepository, never()).saveAndFlush(any());
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void getNotifications_DefaultFilterExcludesArchived() {
        User recipient = user(10L);
        Notification notification = notification(recipient, NotificationStatus.UNREAD);
        PageRequest pageable = PageRequest.of(0, 20);
        when(notificationRepository.findByRecipientIdAndStatusNot(
                recipient.getId(), NotificationStatus.ARCHIVED, pageable))
                .thenReturn(new PageImpl<>(List.of(notification)));

        var result = service.getNotifications(recipient, null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(notification.getPublicId(), result.getContent().getFirst().id());
    }

    @Test
    void markAsRead_RejectsNotificationOwnedByAnotherUser() {
        UUID notificationId = UUID.randomUUID();
        User currentUser = user(20L);
        when(notificationRepository.findByPublicIdAndRecipientId(
                notificationId, currentUser.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.markAsRead(notificationId, currentUser));
    }

    @Test
    void markAsRead_TransitionsUnreadNotification() {
        User recipient = user(10L);
        Notification notification = notification(recipient, NotificationStatus.UNREAD);
        when(notificationRepository.findByPublicIdAndRecipientId(
                notification.getPublicId(), recipient.getId()))
                .thenReturn(Optional.of(notification));

        var response = service.markAsRead(notification.getPublicId(), recipient);

        assertEquals(NotificationStatus.READ, response.status());
        assertNotNull(response.readAt());
        verify(notificationRepository).save(notification);
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setPublicId(UUID.randomUUID());
        user.setName("Recipient");
        user.setEmail("recipient" + id + "@test.com");
        return user;
    }

    private Notification notification(User recipient, NotificationStatus status) {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setPublicId(UUID.randomUUID());
        notification.setRecipient(recipient);
        notification.setType(NotificationType.EVENT_UPDATE);
        notification.setTitle("Title");
        notification.setMessage("Message");
        notification.setStatus(status);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setUpdatedAt(LocalDateTime.now());
        return notification;
    }
}
