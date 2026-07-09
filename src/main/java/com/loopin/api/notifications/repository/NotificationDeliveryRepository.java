package com.loopin.api.notifications.repository;

import com.loopin.api.notifications.enums.NotificationDeliveryStatus;
import com.loopin.api.notifications.entity.NotificationDelivery;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    @EntityGraph(attributePaths = {"notification", "notification.recipient"})
    List<NotificationDelivery> findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            NotificationDeliveryStatus status,
            LocalDateTime nextAttemptAt,
            Pageable pageable);
}
