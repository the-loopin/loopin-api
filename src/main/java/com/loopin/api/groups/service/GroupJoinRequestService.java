package com.loopin.api.core.groups.service;

import com.loopin.api.core.groups.dto.request.CreateGroupJoinRequestRequest;
import com.loopin.api.core.groups.dto.response.GroupJoinRequestResponse;

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
