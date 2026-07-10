package com.loopin.api.groups.service;

import com.loopin.api.groups.dto.request.CreateGroupRequest;
import com.loopin.api.groups.dto.request.UpdateGroupRequest;
import com.loopin.api.groups.dto.request.UpdateGroupStatusRequest;
import com.loopin.api.groups.dto.response.GroupResponse;

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
