package com.loopin.api.core.groups.service;

import com.loopin.api.core.groups.enums.GroupStatus;
import com.loopin.api.core.groups.enums.RequestStatus;
import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.core.groups.dto.request.CreateGroupJoinRequestRequest;
import com.loopin.api.core.groups.dto.response.GroupJoinRequestResponse;
import com.loopin.api.core.events.entity.EventGroup;
import com.loopin.api.core.groups.entity.GroupJoinRequest;
import com.loopin.api.core.groups.entity.GroupMember;
import com.loopin.api.core.users.entity.User;
import com.loopin.api.core.events.repository.EventGroupRepository;
import com.loopin.api.core.groups.repository.GroupJoinRequestRepository;
import com.loopin.api.core.groups.repository.GroupMemberRepository;
import com.loopin.api.core.users.repository.UserRepository;
import com.loopin.api.notifications.service.NotificationService;
import com.loopin.api.moderation.ContentModerationProperties;
import com.loopin.api.moderation.ContentModerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupJoinRequestServiceImplTest {

    private static final UUID GROUP_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID REQUEST_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Long USER_ID = 100L;
    private static final Long ADMIN_ID = 200L;
    private static final Long GROUP_INTERNAL_ID = 10L;

    private GroupJoinRequestRepository joinRequestRepository;
    private EventGroupRepository eventGroupRepository;
    private UserRepository userRepository;
    private GroupMemberRepository groupMemberRepository;
    private NotificationService notificationService;

    private GroupJoinRequestServiceImpl joinRequestService;

    @BeforeEach
    void setUp() {
        joinRequestRepository = mock(GroupJoinRequestRepository.class);
        eventGroupRepository = mock(EventGroupRepository.class);
        userRepository = mock(UserRepository.class);
        groupMemberRepository = mock(GroupMemberRepository.class);
        notificationService = mock(NotificationService.class);

        joinRequestService = new GroupJoinRequestServiceImpl(
                joinRequestRepository,
                eventGroupRepository,
                userRepository,
                groupMemberRepository,
                notificationService,
                new ContentModerationService(new ContentModerationProperties())
        );
    }

    @Test
    void create_ValidRequest_CreatesAndReturnsResponse() {
        EventGroup group = group(GROUP_INTERNAL_ID, GROUP_ID, GroupStatus.OPEN, ADMIN_ID, 10);
        User user = user(USER_ID);

        when(eventGroupRepository.findByPublicId(GROUP_ID)).thenReturn(Optional.of(group));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(groupMemberRepository.existsByGroupIdAndUserId(GROUP_INTERNAL_ID, USER_ID)).thenReturn(false);
        when(joinRequestRepository.existsByGroupIdAndUserIdAndStatus(GROUP_INTERNAL_ID, USER_ID, RequestStatus.PENDING)).thenReturn(false);

        GroupJoinRequest savedEntity = request(1L, REQUEST_ID, group, user, RequestStatus.PENDING);
        when(joinRequestRepository.save(any(GroupJoinRequest.class))).thenReturn(savedEntity);

        CreateGroupJoinRequestRequest requestDto = new CreateGroupJoinRequestRequest();
        requestDto.setMessage("Let me in");

        GroupJoinRequestResponse response = joinRequestService.create(GROUP_ID, USER_ID, requestDto);

        verify(joinRequestRepository).save(any(GroupJoinRequest.class));
        verify(notificationService).create(any());
        assertEquals(REQUEST_ID, response.getId());
    }

    @Test
    void create_GroupNotOpen_ThrowsConflict() {
        EventGroup group = group(GROUP_INTERNAL_ID, GROUP_ID, GroupStatus.FULL, ADMIN_ID, 10);
        User user = user(USER_ID);

        when(eventGroupRepository.findByPublicId(GROUP_ID)).thenReturn(Optional.of(group));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        CreateGroupJoinRequestRequest requestDto = new CreateGroupJoinRequestRequest();
        assertThrows(ResponseStatusException.class, () -> joinRequestService.create(GROUP_ID, USER_ID, requestDto));
    }

    @Test
    void create_AlreadyMember_ThrowsConflict() {
        EventGroup group = group(GROUP_INTERNAL_ID, GROUP_ID, GroupStatus.OPEN, ADMIN_ID, 10);
        User user = user(USER_ID);

        when(eventGroupRepository.findByPublicId(GROUP_ID)).thenReturn(Optional.of(group));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(groupMemberRepository.existsByGroupIdAndUserId(GROUP_INTERNAL_ID, USER_ID)).thenReturn(true);

        CreateGroupJoinRequestRequest requestDto = new CreateGroupJoinRequestRequest();
        assertThrows(ResponseStatusException.class, () -> joinRequestService.create(GROUP_ID, USER_ID, requestDto));
    }

    @Test
    void create_PendingRequestExists_ThrowsConflict() {
        EventGroup group = group(GROUP_INTERNAL_ID, GROUP_ID, GroupStatus.OPEN, ADMIN_ID, 10);
        User user = user(USER_ID);

        when(eventGroupRepository.findByPublicId(GROUP_ID)).thenReturn(Optional.of(group));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(groupMemberRepository.existsByGroupIdAndUserId(GROUP_INTERNAL_ID, USER_ID)).thenReturn(false);
        when(joinRequestRepository.existsByGroupIdAndUserIdAndStatus(GROUP_INTERNAL_ID, USER_ID, RequestStatus.PENDING)).thenReturn(true);

        CreateGroupJoinRequestRequest requestDto = new CreateGroupJoinRequestRequest();
        assertThrows(ResponseStatusException.class, () -> joinRequestService.create(GROUP_ID, USER_ID, requestDto));
    }

    @Test
    void create_BlockedMessage_SavesRejectedRequestWithoutNotifyingAdmin() {
        EventGroup group = group(GROUP_INTERNAL_ID, GROUP_ID, GroupStatus.OPEN, ADMIN_ID, 10);
        User user = user(USER_ID);
        user.setPublicId(UUID.randomUUID());
        GroupJoinRequest savedEntity = request(1L, REQUEST_ID, group, user, RequestStatus.REJECTED);

        ContentModerationProperties properties = new ContentModerationProperties();
        properties.setBannedWords(List.of("scam"));
        joinRequestService = new GroupJoinRequestServiceImpl(
                joinRequestRepository, eventGroupRepository, userRepository, groupMemberRepository,
                notificationService, new ContentModerationService(properties));

        when(eventGroupRepository.findByPublicId(GROUP_ID)).thenReturn(Optional.of(group));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(groupMemberRepository.existsByGroupIdAndUserId(GROUP_INTERNAL_ID, USER_ID)).thenReturn(false);
        when(joinRequestRepository.existsByGroupIdAndUserIdAndStatus(GROUP_INTERNAL_ID, USER_ID, RequestStatus.PENDING)).thenReturn(false);
        when(joinRequestRepository.save(any(GroupJoinRequest.class))).thenReturn(savedEntity);

        CreateGroupJoinRequestRequest requestDto = new CreateGroupJoinRequestRequest();
        requestDto.setMessage("This is a SCAM");

        GroupJoinRequestResponse response = joinRequestService.create(GROUP_ID, USER_ID, requestDto);

        assertEquals(RequestStatus.REJECTED, response.getStatus());
        verify(notificationService, never()).create(any());
    }

    @Test
    void approve_Valid_ApprovesAndCreatesMember() {
        EventGroup group = group(GROUP_INTERNAL_ID, GROUP_ID, GroupStatus.OPEN, ADMIN_ID, 10);
        User user = user(USER_ID);
        GroupJoinRequest requestEntity = request(1L, REQUEST_ID, group, user, RequestStatus.PENDING);

        when(eventGroupRepository.findByPublicId(GROUP_ID)).thenReturn(Optional.of(group));
        when(joinRequestRepository.findByPublicIdAndGroupId(REQUEST_ID, GROUP_INTERNAL_ID)).thenReturn(Optional.of(requestEntity));
        when(groupMemberRepository.existsByGroupIdAndUserId(GROUP_INTERNAL_ID, USER_ID)).thenReturn(false);
        when(groupMemberRepository.countByGroupId(GROUP_INTERNAL_ID)).thenReturn(5);

        GroupJoinRequestResponse response = joinRequestService.approve(GROUP_ID, REQUEST_ID, ADMIN_ID);

        assertEquals(RequestStatus.ACCEPTED, requestEntity.getStatus());
        verify(joinRequestRepository).save(requestEntity);
        verify(groupMemberRepository).save(any(GroupMember.class));
        verify(notificationService).create(any());
    }

    @Test
    void reject_Valid_RejectsRequest() {
        EventGroup group = group(GROUP_INTERNAL_ID, GROUP_ID, GroupStatus.OPEN, ADMIN_ID, 10);
        User user = user(USER_ID);
        GroupJoinRequest requestEntity = request(1L, REQUEST_ID, group, user, RequestStatus.PENDING);

        when(eventGroupRepository.findByPublicId(GROUP_ID)).thenReturn(Optional.of(group));
        when(joinRequestRepository.findByPublicIdAndGroupId(REQUEST_ID, GROUP_INTERNAL_ID)).thenReturn(Optional.of(requestEntity));
        when(joinRequestRepository.save(requestEntity)).thenReturn(requestEntity);

        GroupJoinRequestResponse response = joinRequestService.reject(GROUP_ID, REQUEST_ID, ADMIN_ID);

        assertEquals(RequestStatus.REJECTED, requestEntity.getStatus());
        verify(joinRequestRepository).save(requestEntity);
        verify(notificationService).create(any());
    }

    @Test
    void delete_ValidRequester_DeletesRequest() {
        EventGroup group = group(GROUP_INTERNAL_ID, GROUP_ID, GroupStatus.OPEN, ADMIN_ID, 10);
        User user = user(USER_ID);
        GroupJoinRequest requestEntity = request(1L, REQUEST_ID, group, user, RequestStatus.PENDING);

        when(eventGroupRepository.findByPublicId(GROUP_ID)).thenReturn(Optional.of(group));
        when(joinRequestRepository.findByPublicIdAndGroupId(REQUEST_ID, GROUP_INTERNAL_ID)).thenReturn(Optional.of(requestEntity));

        joinRequestService.delete(GROUP_ID, REQUEST_ID, USER_ID);

        verify(joinRequestRepository).delete(requestEntity);
    }

    @Test
    void delete_NotAdminOrRequester_ThrowsForbidden() {
        EventGroup group = group(GROUP_INTERNAL_ID, GROUP_ID, GroupStatus.OPEN, ADMIN_ID, 10);
        User user = user(USER_ID);
        GroupJoinRequest requestEntity = request(1L, REQUEST_ID, group, user, RequestStatus.PENDING);

        when(eventGroupRepository.findByPublicId(GROUP_ID)).thenReturn(Optional.of(group));
        when(joinRequestRepository.findByPublicIdAndGroupId(REQUEST_ID, GROUP_INTERNAL_ID)).thenReturn(Optional.of(requestEntity));

        assertThrows(ResponseStatusException.class, () -> joinRequestService.delete(GROUP_ID, REQUEST_ID, 999L));
    }

    private EventGroup group(Long id, UUID publicId, GroupStatus status, Long adminId, int maxMembers) {
        EventGroup group = new EventGroup();
        group.setId(id);
        group.setPublicId(publicId);
        group.setStatus(status);
        group.setMaxMembers(maxMembers);

        User admin = new User();
        admin.setId(adminId);
        group.setAdmin(admin);

        return group;
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private GroupJoinRequest request(Long id, UUID publicId, EventGroup group, User user, RequestStatus status) {
        GroupJoinRequest request = new GroupJoinRequest();
        request.setId(id);
        request.setPublicId(publicId);
        request.setGroup(group);
        request.setUser(user);
        request.setStatus(status);
        return request;
    }
}
