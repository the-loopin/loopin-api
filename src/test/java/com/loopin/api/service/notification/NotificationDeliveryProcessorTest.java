package com.loopin.api.service.notification;

import com.loopin.api.common.enums.NotificationDeliveryStatus;
import com.loopin.api.config.N8nNotificationProperties;
import com.loopin.api.entity.NotificationDelivery;
import com.loopin.api.repository.NotificationDeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationDeliveryProcessorTest {

    private NotificationDeliveryRepository repository;
    private N8nNotificationClient client;
    private N8nNotificationProperties properties;
    private NotificationDeliveryProcessor processor;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationDeliveryRepository.class);
        client = mock(N8nNotificationClient.class);
        properties = new N8nNotificationProperties();
        properties.setMaxBackoff(Duration.ofHours(1));
        processor = new NotificationDeliveryProcessor(repository, client, properties);
    }

    @Test
    void process_SuccessMarksDeliveryDelivered() throws Exception {
        NotificationDelivery delivery = pendingDelivery();
        LocalDateTime now = LocalDateTime.now();
        when(repository.findById(1L)).thenReturn(Optional.of(delivery));

        processor.process(1L, now);

        assertEquals(NotificationDeliveryStatus.DELIVERED, delivery.getStatus());
        assertEquals(now, delivery.getDeliveredAt());
        verify(repository).save(delivery);
    }

    @Test
    void process_FailureSchedulesRetryWithoutDroppingDelivery() throws Exception {
        NotificationDelivery delivery = pendingDelivery();
        LocalDateTime now = LocalDateTime.now();
        when(repository.findById(1L)).thenReturn(Optional.of(delivery));
        doThrow(new IOException("n8n unavailable")).when(client).deliver(delivery);

        processor.process(1L, now);

        assertEquals(NotificationDeliveryStatus.PENDING, delivery.getStatus());
        assertEquals(1, delivery.getAttemptCount());
        assertEquals(now.plusMinutes(1), delivery.getNextAttemptAt());
        assertNotNull(delivery.getLastError());
        verify(repository).save(delivery);
    }

    private NotificationDelivery pendingDelivery() {
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setId(1L);
        delivery.setStatus(NotificationDeliveryStatus.PENDING);
        return delivery;
    }
}
