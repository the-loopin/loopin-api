package com.loopin.api.groups.service;

import com.loopin.api.groups.enums.GroupSizeType;
import com.loopin.api.groups.enums.GroupStatus;
import com.loopin.api.common.exception.InvalidGroupStateException;
import com.loopin.api.groups.dto.request.CreateGroupRequest;
import com.loopin.api.groups.dto.request.UpdateGroupRequest;
import com.loopin.api.groups.dto.response.GroupResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.entity.GroupMember;
import com.loopin.api.users.entity.User;
import com.loopin.api.groups.mapper.GroupMapper;
import com.loopin.api.groups.repository.EventGroupRepository;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.users.repository.UserRepository;
import com.loopin.api.notifications.service.NotificationService;
import com.loopin.api.moderation.ContentModerationProperties;
import com.loopin.api.moderation.ContentModerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupServiceImplTest {

    private static final UUID GROUP_PUBLIC_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID EVENT_PUBLIC_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID MEMBER_PUBLIC_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private EventGroupRepository eventGroupRepository;
    private GroupMemberRepository groupMemberRepository;
    private GroupMapper groupMapper;
    private EventRepository eventRepository;
    private UserRepository userRepository;
    private NotificationService notificationService;
    private GroupServiceImpl groupService;

    @BeforeEach
    void setUp() {
        eventGroupRepository = mock(EventGroupRepository.class);
        groupMemberRepository = mock(GroupMemberRepository.class);
        groupMapper = mock(GroupMapper.class);
        eventRepository = mock(EventRepository.class);
        userRepository = mock(UserRepository.class);
        notificationService = mock(NotificationService.class);
        groupService = new GroupServiceImpl(
                eventGroupRepository,
                groupMapper,
                groupMemberRepository,
                eventRepository,
                userRepository,
                notificationService,
                new ContentModerationService(new ContentModerationProperties()));
    }

    @Test
    void createGroup_DerivesMaxMembersFromGroupSize() {
        User admin = user(1L, "admin@email.com");
        CreateGroupRequest request = new CreateGroupRequest();
        request.setEventId(EVENT_PUBLIC_ID);
        request.setTitle("Dinner");
        request.setGroupSize(GroupSizeType.FOUR_PLUS);
        request.setMaxMembers(2);

        Event event = new Event();
        EventGroup group = group(admin, GroupSizeType.FOUR_PLUS, GroupStatus.OPEN);

        when(userRepository.findByEmail("admin@email.com")).thenReturn(Optional.of(admin));
        when(eventRepository.findByPublicIdAndDeletedAtIsNull(EVENT_PUBLIC_ID)).thenReturn(Optional.of(event));
        when(groupMapper.toEntity(request, admin, event)).thenReturn(group);
        when(eventGroupRepository.save(any(EventGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(groupMapper.toGroupResponse(group)).thenReturn(mock(GroupResponse.class));

        groupService.createGroup(request, "admin@email.com");

        assertEquals(10, group.getMaxMembers());
        assertEquals(GroupStatus.OPEN, group.getStatus());
    }

    @Test
    void createGroup_BlockedTitle_IsRejectedBeforeAnyPersistence() {
        ContentModerationProperties properties = new ContentModerationProperties();
        properties.setBannedWords(java.util.List.of("scam"));
        groupService = new GroupServiceImpl(
                eventGroupRepository, groupMapper, groupMemberRepository, eventRepository, userRepository,
                notificationService, new ContentModerationService(properties));
        CreateGroupRequest request = new CreateGroupRequest();
        request.setTitle("SCAM meetup");

        assertThrows(IllegalArgumentException.class, () -> groupService.createGroup(request, "admin@email.com"));
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void addMember_MarksGroupFullWhenCapacityIsMet() {
        EventGroup group = group(user(1L, "admin@email.com"), GroupSizeType.TWO, GroupStatus.OPEN);
        User newMember = user(2L, "member@email.com");

        when(eventGroupRepository.findByPublicId(GROUP_PUBLIC_ID)).thenReturn(Optional.of(group));
        when(groupMemberRepository.countByGroupId(1L)).thenReturn(1);
        when(userRepository.findByPublicIdAndDeletedAtIsNull(MEMBER_PUBLIC_ID)).thenReturn(Optional.of(newMember));
        when(groupMemberRepository.existsByGroupIdAndUserId(1L, 2L)).thenReturn(false);

        groupService.addMember(GROUP_PUBLIC_ID, MEMBER_PUBLIC_ID, "admin@email.com");

        assertEquals(GroupStatus.FULL, group.getStatus());
        verify(groupMemberRepository).save(any(GroupMember.class));
        verify(eventGroupRepository).save(group);
    }

    @Test
    void addMember_RejectsFullGroup() {
        EventGroup group = group(user(1L, "admin@email.com"), GroupSizeType.TWO, GroupStatus.FULL);

        when(eventGroupRepository.findByPublicId(GROUP_PUBLIC_ID)).thenReturn(Optional.of(group));
        when(groupMemberRepository.countByGroupId(1L)).thenReturn(2);

        assertThrows(InvalidGroupStateException.class, () -> groupService.addMember(GROUP_PUBLIC_ID, MEMBER_PUBLIC_ID, "admin@email.com"));

        verify(groupMemberRepository, never()).save(any(GroupMember.class));
    }

    @Test
    void addMember_MarksOpenGroupFullAndRejectsWhenAlreadyAtCapacity() {
        EventGroup group = group(user(1L, "admin@email.com"), GroupSizeType.TWO, GroupStatus.OPEN);

        when(eventGroupRepository.findByPublicId(GROUP_PUBLIC_ID)).thenReturn(Optional.of(group));
        when(groupMemberRepository.countByGroupId(1L)).thenReturn(2);

        assertThrows(InvalidGroupStateException.class, () -> groupService.addMember(GROUP_PUBLIC_ID, MEMBER_PUBLIC_ID, "admin@email.com"));

        assertEquals(GroupStatus.FULL, group.getStatus());
        verify(groupMemberRepository, never()).save(any(GroupMember.class));
        verify(eventGroupRepository).save(group);
    }

    @Test
    void addMember_SavesMembershipForRequestedUser() {
        EventGroup group = group(user(1L, "admin@email.com"), GroupSizeType.THREE, GroupStatus.OPEN);
        User newMember = user(2L, "member@email.com");

        when(eventGroupRepository.findByPublicId(GROUP_PUBLIC_ID)).thenReturn(Optional.of(group));
        when(groupMemberRepository.countByGroupId(1L)).thenReturn(1);
        when(userRepository.findByPublicIdAndDeletedAtIsNull(MEMBER_PUBLIC_ID)).thenReturn(Optional.of(newMember));
        when(groupMemberRepository.existsByGroupIdAndUserId(1L, 2L)).thenReturn(false);

        groupService.addMember(GROUP_PUBLIC_ID, MEMBER_PUBLIC_ID, "admin@email.com");

        ArgumentCaptor<GroupMember> memberCaptor = ArgumentCaptor.forClass(GroupMember.class);
        verify(groupMemberRepository).save(memberCaptor.capture());
        verify(notificationService).create(any());
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

        when(eventGroupRepository.findByPublicId(GROUP_PUBLIC_ID)).thenReturn(Optional.of(group));
        when(groupMemberRepository.countByGroupId(1L)).thenReturn(3);
        when(eventGroupRepository.save(group)).thenReturn(group);
        when(groupMapper.toGroupResponse(group)).thenReturn(mock(GroupResponse.class));

        groupService.updateGroup(GROUP_PUBLIC_ID, request, "admin@email.com");

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

        when(eventGroupRepository.findByPublicId(GROUP_PUBLIC_ID)).thenReturn(Optional.of(group));
        when(groupMemberRepository.countByGroupId(1L)).thenReturn(5);

        assertThrows(
                InvalidGroupStateException.class,
                () -> groupService.updateGroup(GROUP_PUBLIC_ID, request, "admin@email.com"));

        verify(eventGroupRepository, never()).save(group);
    }

    @Test
    void removeMember_ReopensFullGroupWhenCapacityIsNoLongerMet() {
        EventGroup group = group(user(1L, "admin@email.com"), GroupSizeType.TWO, GroupStatus.FULL);
        User memberUser = user(2L, "member@email.com");
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(memberUser);

        when(eventGroupRepository.findByPublicId(GROUP_PUBLIC_ID)).thenReturn(Optional.of(group));
        when(userRepository.findByPublicIdAndDeletedAtIsNull(MEMBER_PUBLIC_ID)).thenReturn(Optional.of(memberUser));
        when(groupMemberRepository.findByGroupIdAndUserId(1L, 2L)).thenReturn(Optional.of(member));
        when(groupMemberRepository.countByGroupId(1L)).thenReturn(2);

        groupService.removeMember(GROUP_PUBLIC_ID, MEMBER_PUBLIC_ID, "admin@email.com");

        assertEquals(GroupStatus.OPEN, group.getStatus());
        verify(groupMemberRepository).delete(member);
        verify(eventGroupRepository).save(group);
    }

    private EventGroup group(User admin, GroupSizeType groupSize, GroupStatus status) {
        EventGroup group = new EventGroup();
        group.setId(1L);
        group.setPublicId(GROUP_PUBLIC_ID);
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
        if (id == 2L) {
            user.setPublicId(MEMBER_PUBLIC_ID);
        }
        return user;
    }
}
