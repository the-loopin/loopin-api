package com.loopin.api.events.repository;

import com.loopin.api.events.entity.UserLoopedEvent;
import com.loopin.api.events.repository.projection.LoopedEventCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UserLoopedEventRepository
    extends JpaRepository<UserLoopedEvent, Long> {

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    long countByEventId(Long eventId);

    @Modifying(flushAutomatically = true)
    @Query(
        value = """
                    insert into user_looped_events (
                        public_id,
                        created_at,
                        updated_at,
                        user_id,
                        event_id
                    )
                    values (
                        :publicId,
                        :createdAt,
                        :createdAt,
                        :userId,
                        :eventId
                    )
                    on conflict (user_id, event_id) do nothing
                    """,
        nativeQuery = true
    )
    int insertIfAbsent(
        @Param("publicId") UUID publicId,
        @Param("createdAt") LocalDateTime createdAt,
        @Param("userId") Long userId,
        @Param("eventId") Long eventId
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            delete from UserLoopedEvent loopedEvent
            where loopedEvent.user.id = :userId
              and loopedEvent.event.publicId = :eventPublicId
            """)
    int deleteByUserIdAndEventPublicId(
        @Param("userId") Long userId,
        @Param("eventPublicId") UUID eventPublicId
    );

    @Query("""
            select
                loopedEvent.event.id as eventId,
                count(loopedEvent.id) as loopedCount
            from UserLoopedEvent loopedEvent
            where loopedEvent.event.id in :eventIds
            group by loopedEvent.event.id
            """)
    List<LoopedEventCountProjection> countByEventIds(
        @Param("eventIds") Collection<Long> eventIds
    );

    @Query(
        value = """
                    select loopedEvent
                    from UserLoopedEvent loopedEvent
                    join fetch loopedEvent.event event
                    where loopedEvent.user.id = :userId
                      and event.deletedAt is null
                    order by loopedEvent.createdAt desc
                    """,
        countQuery = """
                    select count(loopedEvent)
                    from UserLoopedEvent loopedEvent
                    join loopedEvent.event event
                    where loopedEvent.user.id = :userId
                      and event.deletedAt is null
                    """
    )
    Page<UserLoopedEvent> findPageByUserId(
        @Param("userId") Long userId,
        Pageable pageable
    );
}
