package com.loopin.api.events.repository;

import com.loopin.api.events.entity.Event;
import com.loopin.api.events.entity.UserLoopedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserLoopedEventRepository extends JpaRepository<UserLoopedEvent, Long> {

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    long countByEventId(Long eventId);

    Optional<UserLoopedEvent> findByUserIdAndEventId(Long userId, Long eventId);

    @Query("""
            select loopedEvent.event.id, count(loopedEvent.id)
            from UserLoopedEvent loopedEvent
            where loopedEvent.event.id in :eventIds
            group by loopedEvent.event.id
            """)
    List<Object[]> countByEventIds(@Param("eventIds") List<Long> eventIds);

    @Query("""
            select distinct event
            from UserLoopedEvent loopedEvent
            join loopedEvent.event event
            left join fetch event.interests eventInterest
            left join fetch eventInterest.interest
            where loopedEvent.user.id = :userId
              and event.deletedAt is null
            order by loopedEvent.createdAt desc
            """)
    List<Event> findLoopedEventsByUserId(@Param("userId") Long userId);
}
