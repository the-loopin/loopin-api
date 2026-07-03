package com.loopin.api.service.implementation;

import com.loopin.api.common.enums.GroupSizeType;
import com.loopin.api.common.enums.GroupStatus;
import com.loopin.api.common.exception.InvalidGroupStateException;
import com.loopin.api.dto.group.request.CreateGroupRequest;
import com.loopin.api.dto.group.request.UpdateGroupRequest;
import com.loopin.api.dto.group.response.GroupResponse;
import com.loopin.api.entity.EventGroup;
import com.loopin.api.entity.GroupMember;
import com.loopin.api.entity.User;
import com.loopin.api.mapper.GroupMapper;
import com.loopin.api.repository.EventGroupRepository;
import com.loopin.api.repository.EventRepository;
import com.loopin.api.repository.GroupMemberRepository;
import com.loopin.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupServiceImplTest {

    private EventGroupRepository eventGroupRepository;
    private GroupMemberRepository groupMemberRepository;
    private GroupMapper groupMapper;
    private UserRepository userRepository;
    private GroupServiceImpl groupService;

    @BeforeEach
    void setUp() {
        eventGroupRepository = mock(EventGroupRepository.class);
        groupMemberRepository = mock(GroupMemberRepository.class);
        groupMapper = mock(GroupMapper.class);
        EventRepository eventRepository = mock(EventRepository.class);
        userRepository = mock(UserRepository.class);
        groupService = new GroupServiceImpl(
                eventGroupRepository,
                groupMapper,
                groupMemberRepository,
                eventRepository,
                userRepository);
    }

    @Test
    void createGroup_DerivesMaxMembersFromGroupSize() {
        User admin = user(1L, "admin@email.com");
        CreateGroupRequest request = new CreateGroupRequest();
        request.setTitle("Dinner");
        request.setGroupSize(GroupSizeType.FOUR_PLUS);
        request.setMaxMembers(2);

        EventGroup group = group(admin, GroupSizeType.FOUR_PLUS, GroupStatus.OPEN);

        when(userRepository.findByEmail("admin@email.com")).thenReturn(Optional.of(admin));
        when(groupMapper.toEntity(request, admin, null)).thenReturn(group);
        when(eventGroupRepository.save(any(EventGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(groupMapper.toGroupResponse(group)).thenReturn(mock(GroupResponse.class));

        groupService.createGroup(request, "admin@email.com");

        assertEquals(10, group.getMaxMembers());
        assertEquals(GroupStatus.OPEN, group.getStatus());
    }

    @Test
    void addMember_MarksGroupFullWhenCapacityIsMet() {
        EventGroup group = group(user(1L, "admin@email.com"), GroupSizeType.TWO, GroupStatus.OPEN);
        User newMember = user(2L, "member@email.com");

        when(eventGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.countByGroupId(1L)).thenReturn(1);
        when(userRepository.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(newMember));
        when(groupMemberRepository.existsByGroupIdAndUserId(1L, 2L)).thenReturn(false);

        groupService.addMember(1L, 2L);

        assertEquals(GroupStatus.FULL, group.getStatus());
        verify(groupMemberRepository).save(any(GroupMember.class));
        verify(eventGroupRepository).save(group);
    }

    @Test
    void addMember_RejectsFullGroup() {
        EventGroup group = group(user(1L, "admin@email.com"), GroupSizeType.TWO, GroupStatus.FULL);

        when(eventGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.countByGroupId(1L)).thenReturn(2);

        assertThrows(InvalidGroupStateException.class, () -> groupService.addMember(1L, 2L));

        verify(groupMemberRepository, never()).save(any(GroupMember.class));
    }

    @Test
    void addMember_MarksOpenGroupFullAndRejectsWhenAlreadyAtCapacity() {
        EventGroup group = group(user(1L, "admin@email.com"), GroupSizeType.TWO, GroupStatus.OPEN);

        when(eventGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.countByGroupId(1L)).thenReturn(2);

        assertThrows(InvalidGroupStateException.class, () -> groupService.addMember(1L, 2L));

        assertEquals(GroupStatus.FULL, group.getStatus());
        verify(groupMemberRepository, never()).save(any(GroupMember.class));
        verify(eventGroupRepository).save(group);
    }

    @Test
    void addMember_SavesMembershipForRequestedUser() {
        EventGroup group = group(user(1L, "admin@email.com"), GroupSizeType.THREE, GroupStatus.OPEN);
        User newMember = user(2L, "member@email.com");

        when(eventGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.countByGroupId(1L)).thenReturn(1);
        when(userRepository.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(newMember));
        when(groupMemberRepository.existsByGroupIdAndUserId(1L, 2L)).thenReturn(false);

        groupService.addMember(1L, 2L);

        ArgumentCaptor<GroupMember> memberCaptor = ArgumentCaptor.forClass(GroupMember.class);
        verify(groupMemberRepository).save(memberCaptor.capture());
        assertEquals(group, memberCaptor.getValue().getGroup());
        assertEquals(newMember, memberCaptor.getValue().getUser());
        assertEquals(GroupStatus.OPEN, group.getStatus());
    }

    @Test
    void updateGroup_DerivesCapacityFromUpdatedGroupSizeAndMarksFull() {
        User admin = user(1L, "admin@email.com");
        EventGroup group = group(admin, GroupSizeType.FOUR, GroupStatus.OPEN);
        UpdateGroupRequest request = new UpdateGroupRequest();
        request.setGroupSize(GroupSizeType.THREE);

        when(eventGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.countByGroupId(1L)).thenReturn(3);
        when(eventGroupRepository.save(group)).thenReturn(group);
        when(groupMapper.toGroupResponse(group)).thenReturn(mock(GroupResponse.class));

        groupService.updateGroup(1L, request, "admin@email.com");

        assertEquals(GroupSizeType.THREE, group.getGroupSize());
        assertEquals(3, group.getMaxMembers());
        assertEquals(GroupStatus.FULL, group.getStatus());
        verify(eventGroupRepository, times(2)).save(group);
    }

    @Test
    void updateGroup_RejectsGroupSizeBelowCurrentMemberCount() {
        User admin = user(1L, "admin@email.com");
        EventGroup group = group(admin, GroupSizeType.FOUR_PLUS, GroupStatus.OPEN);
        UpdateGroupRequest request = new UpdateGroupRequest();
        request.setGroupSize(GroupSizeType.FOUR);

        when(eventGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.countByGroupId(1L)).thenReturn(5);

        assertThrows(
                InvalidGroupStateException.class,
                () -> groupService.updateGroup(1L, request, "admin@email.com"));

        verify(eventGroupRepository, never()).save(group);
    }

    @Test
    void removeMember_ReopensFullGroupWhenCapacityIsNoLongerMet() {
        EventGroup group = group(user(1L, "admin@email.com"), GroupSizeType.TWO, GroupStatus.FULL);
        User memberUser = user(2L, "member@email.com");
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(memberUser);

        when(eventGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(1L, 2L)).thenReturn(Optional.of(member));
        when(groupMemberRepository.countByGroupId(1L)).thenReturn(2);

        groupService.removeMember(1L, 2L);

        assertEquals(GroupStatus.OPEN, group.getStatus());
        verify(groupMemberRepository).delete(member);
        verify(eventGroupRepository).save(group);
    }

    private EventGroup group(User admin, GroupSizeType groupSize, GroupStatus status) {
        EventGroup group = new EventGroup();
        group.setId(1L);
        group.setAdmin(admin);
        group.setTitle("Test Group");
        group.setGroupSize(groupSize);
        group.setStatus(status);
        group.setMaxMembers(groupSize.getMaxMembers());
        return group;
    }

    private User user(Long id, String email) {
        User user = new User(email, "Test User", null);
        user.setId(id);
        return user;
    }
}
