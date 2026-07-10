package com.loopin.api.core.interests.repository;

import com.loopin.api.core.interests.entity.UserInterest;
import com.loopin.api.core.interests.entity.UserInterestId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserInterestRepository extends JpaRepository<UserInterest, UserInterestId> {

    List<UserInterest> findByUser_Id(Long userId);

    @Modifying
    void deleteByUser_Id(Long userId);

    @Query("""
            select eventInterest.event.id
            from UserInterest userInterest
            join EventInterest eventInterest on eventInterest.interest.id = userInterest.interest.id
            where userInterest.user.id = :userId
            group by eventInterest.event.id
            order by sum(userInterest.weight) desc, count(eventInterest.interest.id) desc
            """)
    List<Long> findRecommendedEventIdsByUserInterestOverlap(Long userId);
}
