package com.loopin.api.notifications.job;

import com.loopin.api.notifications.enums.NotificationDeliveryStatus;
import com.loopin.api.common.config.N8nNotificationProperties;
import com.loopin.api.notifications.entity.NotificationDelivery;
import com.loopin.api.notifications.repository.NotificationDeliveryRepository;
import com.loopin.api.notifications.service.NotificationDeliveryProcessor;
import com.loopin.api.core.events.job.EventCompletionJobLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDeliveryJob {

    private static final String LOCK_NAME = "notification-delivery";

    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationDeliveryProcessor deliveryProcessor;
    private final EventCompletionJobLockService jobLockService;
    private final N8nNotificationProperties properties;

    @Scheduled(
            fixedDelayString = "${loopin.notifications.n8n.worker-delay-ms:10000}",
            initialDelayString = "${loopin.notifications.n8n.worker-initial-delay-ms:10000}")
    public void deliverPending() {
        if (!properties.isEnabled()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (!jobLockService.acquireLock(LOCK_NAME, now, Duration.ofMinutes(5))) {
            return;
        }

        try {
            List<NotificationDelivery> due = deliveryRepository
                    .findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                            NotificationDeliveryStatus.PENDING,
                            now,
                            PageRequest.of(0, properties.getBatchSize()));
            due.forEach(delivery -> deliveryProcessor.process(delivery.getId(), now));
        } finally {
            jobLockService.releaseLock(LOCK_NAME, LocalDateTime.now());
        }
    }
}
