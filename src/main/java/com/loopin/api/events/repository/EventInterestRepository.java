package com.loopin.api.events.repository;

import com.loopin.api.events.entity.EventInterest;
import com.loopin.api.events.entity.EventInterestId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface EventInterestRepository extends JpaRepository<EventInterest, EventInterestId> {

    List<EventInterest> findByEvent_Id(Long eventId);

    @Modifying
    void deleteByEvent_Id(Long eventId);

    @Query("""
            select eventInterest.event.id
            from EventInterest eventInterest
            where eventInterest.interest.id in :interestIds
            group by eventInterest.event.id
            order by count(eventInterest.interest.id) desc
            """)
    List<Long> findEventIdsByInterestOverlap(Collection<Long> interestIds);
}
