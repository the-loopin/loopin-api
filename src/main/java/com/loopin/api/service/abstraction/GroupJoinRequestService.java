package com.loopin.api.service.abstraction;

import com.loopin.api.dto.group.request.GroupJoinRequestRequest;
import com.loopin.api.dto.group.response.GroupJoinRequestResponse;

import java.util.List;

public interface GroupJoinRequestService {
    public GroupJoinRequestResponse create(GroupJoinRequestRequest dto);
    public GroupJoinRequestResponse getById(Long id);
    public List<GroupJoinRequestResponse> getByGroupId(Long groupId);
    public List<GroupJoinRequestResponse> getByUserId(Long userId);
    public GroupJoinRequestResponse approve(Long id);
    public GroupJoinRequestResponse reject(Long id);
    public void delete(Long id);
}
