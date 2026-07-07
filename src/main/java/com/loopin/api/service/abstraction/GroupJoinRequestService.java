package com.loopin.api.service.abstraction;

import com.loopin.api.dto.group.request.CreateGroupJoinRequestRequest;
import com.loopin.api.dto.group.response.GroupJoinRequestResponse;

import java.util.List;
import java.util.UUID;

public interface GroupJoinRequestService {
    GroupJoinRequestResponse create(UUID groupId, Long currentUserId, CreateGroupJoinRequestRequest request);

    GroupJoinRequestResponse getById(UUID groupId, UUID requestId, Long currentUserId);

    List<GroupJoinRequestResponse> getByGroupId(UUID groupId, Long currentUserId);

    List<GroupJoinRequestResponse> getByUserId(Long currentUserId);

    GroupJoinRequestResponse approve(UUID groupId, UUID requestId, Long currentUserId);

    GroupJoinRequestResponse reject(UUID groupId, UUID requestId, Long currentUserId);

    void delete(UUID groupId, UUID requestId, Long currentUserId);
}
