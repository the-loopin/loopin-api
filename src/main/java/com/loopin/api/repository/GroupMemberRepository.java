package com.loopin.api.repository;


import com.loopin.api.entity.EventGroup;
import com.loopin.api.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    List<GroupMember> findByGroup_Id(Long groupId);
    List<GroupMember> findByUser_Id(Long userId);

    Optional<GroupMember> findByGroup_IdAndUser_Id(Long groupId, Long userId);

    boolean existsByGroup_IdAndUser_Id(Long groupId, Long userId);

    long countByGroup_Id(Long groupId);

    void deleteByGroup_IdAndUser_Id(Long groupId, Long userId);

    int countByGroupId(Long groupId);
}