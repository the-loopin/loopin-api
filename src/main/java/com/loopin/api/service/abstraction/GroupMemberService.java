package com.loopin.api.service.abstraction;


import com.loopin.api.dto.group.request.GroupMemberRequest;
import com.loopin.api.dto.group.response.GroupMemberResponse;

import java.util.List;

public interface GroupMemberService {
    GroupMemberResponse getByGroupIdAndUserId(Long groupId, Long userId);
    List<GroupMemberResponse> getByGroupId(Long groupId);
    void removeMember(Long groupId, Long userId);
    GroupMemberResponse addMember(Long groupId, GroupMemberRequest dto);
}
