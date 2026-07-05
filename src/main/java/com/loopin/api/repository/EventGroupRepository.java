package com.loopin.api.repository;


import com.loopin.api.common.enums.GroupStatus;
import com.loopin.api.entity.EventGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventGroupRepository extends JpaRepository<EventGroup, Long> {

    List<EventGroup> findByEventId(Long eventId);

    List<EventGroup> findByEventIdAndStatusNot(Long eventId, GroupStatus status);
}
