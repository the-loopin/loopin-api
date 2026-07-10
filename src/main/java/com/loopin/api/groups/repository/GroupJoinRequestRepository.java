package com.loopin.api.groups.repository;


import com.loopin.api.groups.enums.RequestStatus;
import com.loopin.api.groups.entity.GroupJoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequest, Long> {

    List<GroupJoinRequest> findByGroupId(Long groupId);

    List<GroupJoinRequest> findByUserId(Long userId);

    List<GroupJoinRequest> findByGroupIdAndStatus(Long groupId, RequestStatus status);

    Optional<GroupJoinRequest> findByIdAndGroupId(Long id, Long groupId);

    Optional<GroupJoinRequest> findByPublicIdAndGroupId(UUID publicId, Long groupId);

    Optional<GroupJoinRequest> findByGroupIdAndUserId(Long groupId, Long userId);

    boolean existsByGroupIdAndUserIdAndStatus(Long groupId, Long userId, RequestStatus status);

    void deleteByGroupId(Long groupId);
}
