package com.loopin.api.core.events.repository;


import com.loopin.api.core.groups.enums.GroupStatus;
import com.loopin.api.core.events.entity.EventGroup;
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
