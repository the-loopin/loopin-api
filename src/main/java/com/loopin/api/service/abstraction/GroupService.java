package com.loopin.api.service.abstraction;

import com.loopin.api.dto.request.CreateGroupRequest;
import com.loopin.api.dto.request.UpdateGroupRequest;
import com.loopin.api.dto.request.UpdateGroupStatusRequest;
import com.loopin.api.dto.response.GroupResponse;

public interface GroupService {
    public GroupResponse createGroup(CreateGroupRequest request, String currentUsername);
    public void addMember(Long groupId, Long userId);
    public void removeMember(Long groupId, Long userId);
    public GroupResponse updateGroup(Long groupId, UpdateGroupRequest request, String currentUsername);
    public GroupResponse updateGroupStatus(
            Long groupId,
            UpdateGroupStatusRequest request,
            String currentUsername);
}
