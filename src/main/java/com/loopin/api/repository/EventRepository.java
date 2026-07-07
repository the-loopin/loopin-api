package com.loopin.api.repository;

import com.loopin.api.common.enums.EventStatus;
import com.loopin.api.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

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

    @Query("""
            select distinct event
            from Event event
            left join fetch event.interests ei
            left join fetch ei.interest
            where event.id in :ids
            """)
    List<Event> findAllByIdWithInterests(@Param("ids") List<Long> ids);

    @Query("""
            select distinct event
            from Event event
            left join fetch event.interests ei
            left join fetch ei.interest
            where event.id in :ids
              and event.status = com.loopin.api.common.enums.EventStatus.PUBLISHED
              and event.deletedAt is null
            """)
    List<Event> findPublishedByIdInWithInterests(@Param("ids") List<Long> ids);

    @Query("""
            select distinct event
            from Event event
            left join fetch event.interests ei
            left join fetch ei.interest
            where event.publicId = :publicId
              and event.status = com.loopin.api.common.enums.EventStatus.PUBLISHED
              and event.deletedAt is null
            """)
    Optional<Event> findPublishedByPublicIdWithInterests(@Param("publicId") UUID publicId);
}
