package com.loopin.api.repository;


import com.loopin.api.common.enums.RequestStatus;
import com.loopin.api.entity.GroupJoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequest, Long> {

    List<GroupJoinRequest> findByGroupId(Long groupId);

    List<GroupJoinRequest> findByUserId(Long userId);

    List<GroupJoinRequest> findByGroupIdAndStatus(Long groupId, RequestStatus status);

    Optional<GroupJoinRequest> findByIdAndGroupId(Long id, Long groupId);

    Optional<GroupJoinRequest> findByGroupIdAndUserId(Long groupId, Long userId);

    boolean existsByGroupIdAndUserIdAndStatus(Long groupId, Long userId, RequestStatus status);
}
