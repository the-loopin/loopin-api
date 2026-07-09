package com.loopin.api.notifications.service;

import com.loopin.api.notifications.enums.NotificationDeliveryStatus;
import com.loopin.api.common.config.N8nNotificationProperties;
import com.loopin.api.notifications.entity.NotificationDelivery;
import com.loopin.api.notifications.repository.NotificationDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationDeliveryProcessor {

    private final NotificationDeliveryRepository deliveryRepository;
    private final N8nNotificationClient n8nClient;
    private final N8nNotificationProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long deliveryId, LocalDateTime now) {
        NotificationDelivery delivery = deliveryRepository.findById(deliveryId).orElse(null);
        if (delivery == null || delivery.getStatus() != NotificationDeliveryStatus.PENDING) {
            return;
        }

        try {
            n8nClient.deliver(delivery);
            delivery.setStatus(NotificationDeliveryStatus.DELIVERED);
            delivery.setDeliveredAt(now);
            delivery.setLastError(null);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            scheduleRetry(delivery, now, exception);
        } catch (RuntimeException | java.io.IOException exception) {
            scheduleRetry(delivery, now, exception);
        }
        deliveryRepository.save(delivery);
    }

    private void scheduleRetry(
            NotificationDelivery delivery,
            LocalDateTime now,
            Exception exception) {
        int attempt = delivery.getAttemptCount() + 1;
        delivery.setAttemptCount(attempt);
        long multiplier = 1L << Math.min(attempt - 1, 20);
        Duration delay = Duration.ofMinutes(multiplier);
        if (delay.compareTo(properties.getMaxBackoff()) > 0) {
            delay = properties.getMaxBackoff();
        }
        delivery.setNextAttemptAt(now.plus(delay));
        String message = exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
        delivery.setLastError(message.substring(0, Math.min(message.length(), 2000)));
    }
}
