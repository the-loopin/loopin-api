package com.loopin.api.groups.service;


import com.loopin.api.groups.dto.request.GroupMemberRequest;
import com.loopin.api.groups.dto.response.GroupMemberResponse;

import java.util.List;
import java.util.UUID;

public interface GroupMemberService {
    GroupMemberResponse getByGroupIdAndUserId(UUID groupId, UUID userId);
    List<GroupMemberResponse> getByGroupId(UUID groupId);
    void removeMember(UUID groupId, UUID userId, String currentUsername);
    GroupMemberResponse addMember(UUID groupId, GroupMemberRequest dto, String currentUsername);
}
