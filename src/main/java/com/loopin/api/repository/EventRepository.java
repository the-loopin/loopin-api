package com.loopin.api.repository;

import com.loopin.api.common.enums.EventStatus;
import com.loopin.api.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    long countByStatus(EventStatus status);

    Page<Event> findByStatus(EventStatus status, Pageable pageable);
}