package com.loopin.api.groups.api;

import com.loopin.api.groups.entity.GroupMember;
import com.loopin.api.users.entity.User;
import java.util.List;

public interface GroupMemberLookup {
    List<User> findActiveUsersByEventId(Long eventId);
    List<GroupMember> findMembersByGroupId(Long groupId);
}
