package com.loopin.api.groups.repository;

import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.enums.GroupStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventGroupRepository extends JpaRepository<EventGroup, Long> {

    List<EventGroup> findByEventId(Long eventId);

    List<EventGroup> findByEventIdAndStatusNot(Long eventId, GroupStatus status);

    List<EventGroup> findByStatusAndUpdatedAtBefore(GroupStatus status, LocalDateTime updatedAt);

    Optional<EventGroup> findByPublicId(UUID publicId);

    boolean existsByPublicId(UUID publicId);
}
