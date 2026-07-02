package com.loopin.api.service.abstraction;

import com.loopin.api.dto.group.request.CreateGroupRequest;
import com.loopin.api.dto.group.request.UpdateGroupRequest;
import com.loopin.api.dto.group.request.UpdateGroupStatusRequest;
import com.loopin.api.dto.group.response.GroupResponse;

import java.util.List;

public interface GroupService {
    public GroupResponse createGroup(CreateGroupRequest request, String currentUsername);
    public List<GroupResponse> getAllGroups(Long eventId);
    public GroupResponse updateGroup(Long groupId, UpdateGroupRequest request, String currentUsername);
    public GroupResponse updateGroupStatus(
            Long groupId,
            UpdateGroupStatusRequest request,
            String currentUsername);
}
