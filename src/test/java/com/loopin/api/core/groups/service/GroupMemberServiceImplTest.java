package com.loopin.api.core.groups.service;

import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.groups.dto.request.GroupMemberRequest;
import com.loopin.api.groups.dto.response.GroupMemberResponse;
import com.loopin.api.events.entity.EventGroup;
import com.loopin.api.groups.entity.GroupMember;
import com.loopin.api.groups.service.GroupMemberServiceImpl;
import com.loopin.api.users.entity.User;
import com.loopin.api.groups.mapper.GroupMemberMapper;
import com.loopin.api.events.repository.EventGroupRepository;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.users.repository.UserRepository;
import com.loopin.api.groups.service.GroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupMemberServiceImplTest {

    private static final UUID GROUP_PUBLIC_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MEMBER_PUBLIC_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SECOND_MEMBER_PUBLIC_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID MEMBERSHIP_PUBLIC_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private GroupService groupService;
    private GroupMemberRepository groupMemberRepository;
    private EventGroupRepository eventGroupRepository;
    private UserRepository userRepository;
    private GroupMemberServiceImpl groupMemberService;

    @BeforeEach
    void setUp() {
        groupService = mock(GroupService.class);
        groupMemberRepository = mock(GroupMemberRepository.class);
        eventGroupRepository = mock(EventGroupRepository.class);
        userRepository = mock(UserRepository.class);
        groupMemberService = new GroupMemberServiceImpl(
                groupService,
                groupMemberRepository,
                eventGroupRepository,
                userRepository,
                new GroupMemberMapper());
    }

    @Test
    void addMember_DelegatesToGroupServiceAndReturnsScopedMembership() {
        GroupMemberRequest request = new GroupMemberRequest();
        request.setUserId(MEMBER_PUBLIC_ID);
        GroupMember membership = membership(10L, 1L, 2L);

        when(eventGroupRepository.findByPublicId(GROUP_PUBLIC_ID))
                .thenReturn(Optional.of(membership.getGroup()));
        when(userRepository.findByPublicIdAndDeletedAtIsNull(MEMBER_PUBLIC_ID))
                .thenReturn(Optional.of(membership.getUser()));
        when(groupMemberRepository.findByGroupIdAndUserId(1L, 2L))
                .thenReturn(Optional.of(membership));

        GroupMemberResponse response = groupMemberService.addMember(GROUP_PUBLIC_ID, request, "admin@email.com");

        verify(groupService).addMember(GROUP_PUBLIC_ID, MEMBER_PUBLIC_ID, "admin@email.com");
        assertEquals(MEMBERSHIP_PUBLIC_ID, response.getId());
        assertEquals(GROUP_PUBLIC_ID, response.getGroupId());
        assertEquals(MEMBER_PUBLIC_ID, response.getUserId());
    }

    @Test
    void removeMember_DelegatesToGroupService() {
        groupMemberService.removeMember(GROUP_PUBLIC_ID, MEMBER_PUBLIC_ID, "admin@email.com");

        verify(groupService).removeMember(GROUP_PUBLIC_ID, MEMBER_PUBLIC_ID, "admin@email.com");
    }

    @Test
    void getByGroupIdAndUserId_RejectsMembershipOutsideScopedGroup() {
        EventGroup group = new EventGroup();
        group.setId(1L);
        group.setPublicId(GROUP_PUBLIC_ID);
        User user = new User("user2@email.com", "Test User", null);
        user.setId(2L);
        user.setPublicId(MEMBER_PUBLIC_ID);

        when(eventGroupRepository.findByPublicId(GROUP_PUBLIC_ID)).thenReturn(Optional.of(group));
        when(userRepository.findByPublicIdAndDeletedAtIsNull(MEMBER_PUBLIC_ID)).thenReturn(Optional.of(user));
        when(groupMemberRepository.findByGroupIdAndUserId(1L, 2L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> groupMemberService.getByGroupIdAndUserId(GROUP_PUBLIC_ID, MEMBER_PUBLIC_ID));
    }

    @Test
    void getByGroupId_ReturnsMembersForExistingGroup() {
        EventGroup group = new EventGroup();
        group.setId(1L);
        group.setPublicId(GROUP_PUBLIC_ID);
        when(eventGroupRepository.existsByPublicId(GROUP_PUBLIC_ID)).thenReturn(true);
        when(eventGroupRepository.findByPublicId(GROUP_PUBLIC_ID)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupId(1L))
                .thenReturn(List.of(membership(10L, 1L, 2L), membership(11L, 1L, 3L)));

        List<GroupMemberResponse> responses = groupMemberService.getByGroupId(GROUP_PUBLIC_ID);

        assertEquals(2, responses.size());
        assertEquals(MEMBER_PUBLIC_ID, responses.get(0).getUserId());
        assertEquals(SECOND_MEMBER_PUBLIC_ID, responses.get(1).getUserId());
    }

    private GroupMember membership(Long membershipId, Long groupId, Long userId) {
        EventGroup group = new EventGroup();
        group.setId(groupId);
        group.setPublicId(GROUP_PUBLIC_ID);
        User user = new User("user" + userId + "@email.com", "Test User", null);
        user.setId(userId);
        user.setPublicId(userId == 2L ? MEMBER_PUBLIC_ID : SECOND_MEMBER_PUBLIC_ID);

        GroupMember member = new GroupMember();
        member.setId(membershipId);
        member.setPublicId(membershipId == 10L ? MEMBERSHIP_PUBLIC_ID : UUID.fromString("66666666-6666-6666-6666-666666666666"));
        member.setGroup(group);
        member.setUser(user);
        return member;
    }
}
