package com.loopin.api.service.implementation;

import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.dto.group.request.GroupMemberRequest;
import com.loopin.api.dto.group.response.GroupMemberResponse;
import com.loopin.api.entity.EventGroup;
import com.loopin.api.entity.GroupMember;
import com.loopin.api.entity.User;
import com.loopin.api.mapper.GroupMemberMapper;
import com.loopin.api.repository.EventGroupRepository;
import com.loopin.api.repository.GroupMemberRepository;
import com.loopin.api.service.abstraction.GroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupMemberServiceImplTest {

    private GroupService groupService;
    private GroupMemberRepository groupMemberRepository;
    private EventGroupRepository eventGroupRepository;
    private GroupMemberServiceImpl groupMemberService;

    @BeforeEach
    void setUp() {
        groupService = mock(GroupService.class);
        groupMemberRepository = mock(GroupMemberRepository.class);
        eventGroupRepository = mock(EventGroupRepository.class);
        groupMemberService = new GroupMemberServiceImpl(
                groupService,
                groupMemberRepository,
                eventGroupRepository,
                new GroupMemberMapper());
    }

    @Test
    void addMember_DelegatesToGroupServiceAndReturnsScopedMembership() {
        GroupMemberRequest request = new GroupMemberRequest();
        request.setUserId(2L);
        GroupMember membership = membership(10L, 1L, 2L);

        when(groupMemberRepository.findByGroupIdAndUserId(1L, 2L))
                .thenReturn(Optional.of(membership));

        GroupMemberResponse response = groupMemberService.addMember(1L, request, "admin@email.com");

        verify(groupService).addMember(1L, 2L, "admin@email.com");
        assertEquals(10L, response.getId());
        assertEquals(1L, response.getGroupId());
        assertEquals(2L, response.getUserId());
    }

    @Test
    void removeMember_DelegatesToGroupService() {
        groupMemberService.removeMember(1L, 2L, "admin@email.com");

        verify(groupService).removeMember(1L, 2L, "admin@email.com");
    }

    @Test
    void getByGroupIdAndUserId_RejectsMembershipOutsideScopedGroup() {
        when(groupMemberRepository.findByGroupIdAndUserId(1L, 2L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> groupMemberService.getByGroupIdAndUserId(1L, 2L));
    }

    @Test
    void getByGroupId_ReturnsMembersForExistingGroup() {
        when(eventGroupRepository.existsById(1L)).thenReturn(true);
        when(groupMemberRepository.findByGroupId(1L))
                .thenReturn(List.of(membership(10L, 1L, 2L), membership(11L, 1L, 3L)));

        List<GroupMemberResponse> responses = groupMemberService.getByGroupId(1L);

        assertEquals(2, responses.size());
        assertEquals(2L, responses.get(0).getUserId());
        assertEquals(3L, responses.get(1).getUserId());
    }

    private GroupMember membership(Long membershipId, Long groupId, Long userId) {
        EventGroup group = new EventGroup();
        group.setId(groupId);
        User user = new User("user" + userId + "@email.com", "Test User", null);
        user.setId(userId);

        GroupMember member = new GroupMember();
        member.setId(membershipId);
        member.setGroup(group);
        member.setUser(user);
        return member;
    }
}
