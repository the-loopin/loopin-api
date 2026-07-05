package com.loopin.api.service.abstraction;


import com.loopin.api.dto.group.request.GroupMemberRequest;
import com.loopin.api.dto.group.response.GroupMemberResponse;

import java.util.List;
import java.util.UUID;

public interface GroupMemberService {
    GroupMemberResponse getByGroupIdAndUserId(UUID groupId, UUID userId);
    List<GroupMemberResponse> getByGroupId(UUID groupId);
    void removeMember(UUID groupId, UUID userId, String currentUsername);
    GroupMemberResponse addMember(UUID groupId, GroupMemberRequest dto, String currentUsername);
}
