package com.loopin.api.service.abstraction;

import com.loopin.api.dto.group.request.CreateGroupRequest;
import com.loopin.api.dto.group.request.UpdateGroupRequest;
import com.loopin.api.dto.group.request.UpdateGroupStatusRequest;
import com.loopin.api.dto.group.response.GroupResponse;

import java.util.UUID;

public interface GroupService {
    public GroupResponse createGroup(CreateGroupRequest request, String currentUsername);
    public void addMember(UUID groupId, UUID userId, String currentUsername);
    public void removeMember(UUID groupId, UUID userId, String currentUsername);
    public GroupResponse getGroup(UUID groupId);
    public GroupResponse updateGroup(UUID groupId, UpdateGroupRequest request, String currentUsername);
    public GroupResponse updateGroupStatus(
            UUID groupId,
            UpdateGroupStatusRequest request,
            String currentUsername);
}
