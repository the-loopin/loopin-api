package com.loopin.api.groups.repository;


import com.loopin.api.groups.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    List<GroupMember> findByGroupId(Long groupId);

    int countByGroupId(Long groupId);

    void deleteByGroupId(Long groupId);

    @Query("""
            select distinct member.user
              from GroupMember member
             where member.group.event.id = :eventId
               and member.user.deletedAt is null
               and member.user.isActive = true
            """)
    List<com.loopin.api.users.entity.User> findDistinctActiveUsersByEventId(@Param("eventId") Long eventId);
}
