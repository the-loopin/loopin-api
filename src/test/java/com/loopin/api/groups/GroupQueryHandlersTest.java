package com.loopin.api.groups;

import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.entity.GroupMember;
import com.loopin.api.groups.getgroupdetails.GetGroupDetailsHandler;
import com.loopin.api.groups.getgroupdetails.GetGroupDetailsQuery;
import com.loopin.api.groups.getmembershipdetails.GetMembershipDetailsHandler;
import com.loopin.api.groups.getmembershipdetails.GetMembershipDetailsQuery;
import com.loopin.api.groups.listgroupmembers.ListGroupMembersHandler;
import com.loopin.api.groups.listgroupmembers.ListGroupMembersQuery;
import com.loopin.api.groups.mapper.GroupMapper;
import com.loopin.api.groups.mapper.GroupMemberMapper;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.groups.shared.finder.GroupFinder;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GroupQueryHandlersTest {

    @Test
    void queries_returnGroupAndMembershipReadModels() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EventGroup group = new EventGroup();
        group.setId(1L);
        User user = new User("member@loopin.test", "Member", null);
        user.setId(2L);
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);
        GroupFinder finder = mock(GroupFinder.class);
        GroupMemberRepository members = mock(GroupMemberRepository.class);
        GroupMapper groupMapper = mock(GroupMapper.class);
        GroupMemberMapper memberMapper = mock(GroupMemberMapper.class);
        var groupResponse = mock(com.loopin.api.groups.dto.response.GroupResponse.class);
        var memberResponse = mock(com.loopin.api.groups.dto.response.GroupMemberResponse.class);
        when(finder.findGroup(groupId)).thenReturn(group);
        when(finder.findActiveUser(userId)).thenReturn(user);
        when(groupMapper.toGroupResponse(group)).thenReturn(groupResponse);
        when(members.findByGroupId(1L)).thenReturn(List.of(member));
        when(members.findByGroupIdAndUserId(1L, 2L)).thenReturn(java.util.Optional.of(member));
        when(memberMapper.toResponseList(List.of(member))).thenReturn(List.of(memberResponse));
        when(memberMapper.toResponse(member)).thenReturn(memberResponse);

        assertEquals(groupResponse, new GetGroupDetailsHandler(finder, groupMapper)
                .handle(new GetGroupDetailsQuery(groupId)));
        assertEquals(List.of(memberResponse), new ListGroupMembersHandler(finder, members, memberMapper)
                .handle(new ListGroupMembersQuery(groupId)));
        assertEquals(memberResponse, new GetMembershipDetailsHandler(finder, members, memberMapper)
                .handle(new GetMembershipDetailsQuery(groupId, userId)));
    }
}
