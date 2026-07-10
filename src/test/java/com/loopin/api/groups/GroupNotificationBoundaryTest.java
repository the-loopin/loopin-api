package com.loopin.api.groups;

import com.loopin.api.groups.addgroupmember.AddGroupMemberCommand;
import com.loopin.api.groups.addgroupmember.AddGroupMemberHandler;
import com.loopin.api.groups.approvegroupjoinrequest.ApproveGroupJoinRequestCommand;
import com.loopin.api.groups.approvegroupjoinrequest.ApproveGroupJoinRequestHandler;
import com.loopin.api.groups.creategroupjoinrequest.CreateGroupJoinRequestCommand;
import com.loopin.api.groups.creategroupjoinrequest.CreateGroupJoinRequestHandler;
import com.loopin.api.groups.dto.request.CreateGroupJoinRequestRequest;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.entity.GroupJoinRequest;
import com.loopin.api.groups.entity.GroupMember;
import com.loopin.api.groups.enums.GroupStatus;
import com.loopin.api.groups.enums.RequestStatus;
import com.loopin.api.groups.mapper.GroupMemberMapper;
import com.loopin.api.groups.rejectgroupjoinrequest.RejectGroupJoinRequestCommand;
import com.loopin.api.groups.rejectgroupjoinrequest.RejectGroupJoinRequestHandler;
import com.loopin.api.groups.removegroupmember.RemoveGroupMemberCommand;
import com.loopin.api.groups.removegroupmember.RemoveGroupMemberHandler;
import com.loopin.api.groups.repository.EventGroupRepository;
import com.loopin.api.groups.repository.GroupJoinRequestRepository;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.groups.shared.finder.GroupFinder;
import com.loopin.api.groups.shared.joinrequest.GroupJoinRequestFinder;
import com.loopin.api.groups.shared.policy.GroupAdminPolicy;
import com.loopin.api.groups.shared.policy.GroupCapacityPolicy;
import com.loopin.api.groups.shared.policy.GroupMembershipPolicy;
import com.loopin.api.moderation.ContentModerationService;
import com.loopin.api.moderation.ContentModerationDecision;
import com.loopin.api.notifications.api.NotificationWriter;
import com.loopin.api.notifications.service.NotificationCommand;
import com.loopin.api.users.api.UserLookup;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupNotificationBoundaryTest {

    @Test
    void addGroupMember_usesNotificationWriter() {
        NotificationWriter writer = mock(NotificationWriter.class);
        GroupFinder finder = mock(GroupFinder.class);
        GroupMemberRepository members = mock(GroupMemberRepository.class);
        AddGroupMemberHandler handler = new AddGroupMemberHandler(
                mock(EventGroupRepository.class), members, mock(GroupMemberMapper.class),
                finder, mock(GroupAdminPolicy.class), mock(GroupMembershipPolicy.class),
                mock(GroupCapacityPolicy.class), writer);

        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(finder.findGroup(groupId)).thenReturn(group(groupId));
        when(finder.findActiveUser(userId)).thenReturn(user(userId));
        when(members.save(any())).thenReturn(new GroupMember());

        handler.handle(new AddGroupMemberCommand(groupId, userId, "admin"));
        verify(writer).write(any(NotificationCommand.class));
    }

    @Test
    void removeGroupMember_usesNotificationWriter() {
        NotificationWriter writer = mock(NotificationWriter.class);
        GroupFinder finder = mock(GroupFinder.class);
        GroupMemberRepository members = mock(GroupMemberRepository.class);
        RemoveGroupMemberHandler handler = new RemoveGroupMemberHandler(
                mock(EventGroupRepository.class), members,
                finder, mock(GroupAdminPolicy.class), mock(GroupMembershipPolicy.class),
                mock(GroupCapacityPolicy.class), writer);

        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(finder.findGroup(groupId)).thenReturn(group(groupId));
        when(finder.findActiveUser(userId)).thenReturn(user(userId));
        when(members.findByGroupIdAndUserId(any(), any())).thenReturn(Optional.of(new GroupMember()));

        handler.handle(new RemoveGroupMemberCommand(groupId, userId, "admin"));
        verify(writer).write(any(NotificationCommand.class));
    }

    @Test
    void createGroupJoinRequest_usesNotificationWriter() {
        NotificationWriter writer = mock(NotificationWriter.class);
        GroupJoinRequestFinder finder = mock(GroupJoinRequestFinder.class);
        UserLookup userLookup = mock(UserLookup.class);
        GroupJoinRequestRepository requests = mock(GroupJoinRequestRepository.class);
        ContentModerationService moderation = mock(ContentModerationService.class);
        CreateGroupJoinRequestHandler handler = new CreateGroupJoinRequestHandler(
                finder, requests, mock(GroupMemberRepository.class),
                userLookup, mock(GroupMembershipPolicy.class), moderation, writer);

        UUID groupId = UUID.randomUUID();
        when(finder.findGroup(groupId)).thenReturn(group(groupId));
        when(userLookup.findById(1L)).thenReturn(user(UUID.randomUUID()));
        when(moderation.moderate(any())).thenReturn(ContentModerationDecision.approved());
        when(requests.save(any())).thenAnswer(i -> i.getArgument(0));

        CreateGroupJoinRequestRequest joinRequest = new CreateGroupJoinRequestRequest();
        joinRequest.setMessage("Hi");
        handler.handle(new CreateGroupJoinRequestCommand(groupId, 1L, joinRequest));
        verify(writer).write(any(NotificationCommand.class));
    }

    @Test
    void approveGroupJoinRequest_usesNotificationWriter() {
        NotificationWriter writer = mock(NotificationWriter.class);
        GroupJoinRequestFinder finder = mock(GroupJoinRequestFinder.class);
        GroupJoinRequestRepository requests = mock(GroupJoinRequestRepository.class);
        ApproveGroupJoinRequestHandler handler = new ApproveGroupJoinRequestHandler(
                finder, requests, mock(GroupMemberRepository.class), mock(EventGroupRepository.class),
                mock(GroupAdminPolicy.class), mock(GroupMembershipPolicy.class),
                mock(GroupCapacityPolicy.class), writer);

        UUID groupId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        GroupJoinRequest req = new GroupJoinRequest();
        req.setStatus(RequestStatus.PENDING);
        req.setGroup(group(groupId));
        req.setUser(user(UUID.randomUUID()));
        when(finder.findRequest(groupId, reqId)).thenReturn(req);

        handler.handle(new ApproveGroupJoinRequestCommand(groupId, reqId, 2L));
        verify(writer).write(any(NotificationCommand.class));
    }

    @Test
    void rejectGroupJoinRequest_usesNotificationWriter() {
        NotificationWriter writer = mock(NotificationWriter.class);
        GroupJoinRequestFinder finder = mock(GroupJoinRequestFinder.class);
        GroupJoinRequestRepository requests = mock(GroupJoinRequestRepository.class);
        RejectGroupJoinRequestHandler handler = new RejectGroupJoinRequestHandler(
                finder, requests, mock(GroupAdminPolicy.class), writer);

        UUID groupId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        GroupJoinRequest req = new GroupJoinRequest();
        req.setStatus(RequestStatus.PENDING);
        req.setGroup(group(groupId));
        req.setUser(user(UUID.randomUUID()));
        when(finder.findRequest(groupId, reqId)).thenReturn(req);
        when(requests.save(any())).thenReturn(req);

        handler.handle(new RejectGroupJoinRequestCommand(groupId, reqId, 2L));
        verify(writer).write(any(NotificationCommand.class));
    }

    private EventGroup group(UUID id) {
        EventGroup g = new EventGroup();
        g.setPublicId(id);
        g.setStatus(GroupStatus.OPEN);
        g.setAdmin(user(UUID.randomUUID()));
        return g;
    }

    private User user(UUID id) {
        User u = new User();
        u.setPublicId(id);
        return u;
    }
}
