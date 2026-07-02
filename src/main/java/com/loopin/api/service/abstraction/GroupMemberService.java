package com.loopin.api.service.abstraction;


import com.loopin.api.dto.group.request.GroupMemberRequest;
import com.loopin.api.dto.group.response.GroupMemberResponse;
import com.loopin.api.entity.GroupMember;

import java.util.List;

public interface GroupMemberService {
    public GroupMemberResponse getById(Long id);
    public List<GroupMemberResponse> getByGroupId(Long groupId);
    public void removeMember(Long groupId, Long userId);
    public GroupMemberResponse addMember(Long groupId,GroupMemberRequest dto);
}
