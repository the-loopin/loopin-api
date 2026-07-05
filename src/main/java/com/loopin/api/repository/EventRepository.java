package com.loopin.api.repository;

import com.loopin.api.common.enums.EventStatus;
import com.loopin.api.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    long countByStatus(EventStatus status);

    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    Optional<Event> findByPublicIdAndDeletedAtIsNull(UUID publicId);

    List<Event> findByStatusAndEndDateTimeBeforeAndDeletedAtIsNull(EventStatus status, LocalDateTime now);

    @Query("""
            select event.id
            from Event event
            where event.status = :status
              and event.endDateTime < :now
              and event.deletedAt is null
            """)
    List<Long> findIdsByStatusAndEndDateTimeBeforeAndDeletedAtIsNull(EventStatus status, LocalDateTime now);
}
