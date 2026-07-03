package com.loopin.api.service.abstraction;

import com.loopin.api.dto.group.request.CreateGroupJoinRequestRequest;
import com.loopin.api.dto.group.response.GroupJoinRequestResponse;

import java.util.List;

public interface GroupJoinRequestService {
    GroupJoinRequestResponse create(Long groupId, Long currentUserId, CreateGroupJoinRequestRequest request);

    GroupJoinRequestResponse getById(Long groupId, Long requestId, Long currentUserId);

    List<GroupJoinRequestResponse> getByGroupId(Long groupId, Long currentUserId);

    List<GroupJoinRequestResponse> getByUserId(Long currentUserId);

    GroupJoinRequestResponse approve(Long groupId, Long requestId, Long currentUserId);

    GroupJoinRequestResponse reject(Long groupId, Long requestId, Long currentUserId);

    void delete(Long groupId, Long requestId, Long currentUserId);
}
