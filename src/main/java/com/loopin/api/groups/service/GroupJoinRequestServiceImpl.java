package com.loopin.api.groups.service;

import com.loopin.api.groups.enums.GroupStatus;
import com.loopin.api.groups.enums.RequestStatus;
import com.loopin.api.notifications.enums.NotificationReferenceType;
import com.loopin.api.notifications.enums.NotificationType;
import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.groups.dto.request.CreateGroupJoinRequestRequest;
import com.loopin.api.groups.dto.response.GroupJoinRequestResponse;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.entity.GroupJoinRequest;
import com.loopin.api.groups.entity.GroupMember;
import com.loopin.api.users.entity.User;
import com.loopin.api.groups.repository.EventGroupRepository;
import com.loopin.api.groups.repository.GroupJoinRequestRepository;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.groups.shared.policy.GroupAdminPolicy;
import com.loopin.api.groups.shared.policy.GroupCapacityPolicy;
import com.loopin.api.groups.shared.policy.GroupMembershipPolicy;
import com.loopin.api.users.repository.UserRepository;
import com.loopin.api.groups.service.GroupJoinRequestService;
import com.loopin.api.notifications.service.NotificationService;
import com.loopin.api.notifications.service.NotificationCommand;
import com.loopin.api.moderation.ContentModerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupJoinRequestServiceImpl implements GroupJoinRequestService {

    private final GroupJoinRequestRepository joinRequestRepository;
    private final EventGroupRepository eventGroupRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final NotificationService notificationService;
    private final ContentModerationService contentModerationService;
    private final GroupAdminPolicy groupAdminPolicy;
    private final GroupMembershipPolicy groupMembershipPolicy;
    private final GroupCapacityPolicy groupCapacityPolicy;

    @Override
    @Transactional
    public GroupJoinRequestResponse create(UUID groupId, Long currentUserId, CreateGroupJoinRequestRequest requestDto) {
        EventGroup group = findGroupById(groupId);
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        if (group.getStatus() != GroupStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Group is not open, join request cannot be sent");
        }
        if (groupMemberRepository.existsByGroupIdAndUserId(group.getId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member of this group");
        }
        if (joinRequestRepository.existsByGroupIdAndUserIdAndStatus(group.getId(), user.getId(), RequestStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A pending request already exists");
        }

        GroupJoinRequest request = new GroupJoinRequest();
        request.setGroup(group);
        request.setUser(user);
        request.setMessage(requestDto.getMessage());
        boolean unsafeContent = !contentModerationService.moderate(requestDto.getMessage()).isApproved();
        request.setStatus(unsafeContent ? RequestStatus.REJECTED : RequestStatus.PENDING);

        GroupJoinRequest saved = joinRequestRepository.save(request);
        if (!unsafeContent) {
            notificationService.create(new NotificationCommand(
                    group.getAdmin(),
                    NotificationType.GROUP_ACTIVITY,
                    "New group join request",
                    user.getName() + " requested to join \"" + group.getTitle() + "\".",
                    NotificationReferenceType.GROUP,
                    group.getPublicId()));
        }
        return GroupJoinRequestResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupJoinRequestResponse getById(UUID groupId, UUID requestId, Long currentUserId) {
        GroupJoinRequest request = findEntityByIdAndGroupId(requestId, groupId);
        validateGroupAdminOrRequester(request, currentUserId);
        return GroupJoinRequestResponse.from(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupJoinRequestResponse> getByGroupId(UUID groupId, Long currentUserId) {
        EventGroup group = findGroupById(groupId);
        validateGroupAdmin(group, currentUserId);

        return joinRequestRepository.findByGroupId(group.getId()).stream()
                .map(GroupJoinRequestResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupJoinRequestResponse> getByUserId(Long currentUserId) {
        return joinRequestRepository.findByUserId(currentUserId).stream()
                .map(GroupJoinRequestResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public GroupJoinRequestResponse approve(UUID groupId, UUID requestId, Long currentUserId) {
        GroupJoinRequest request = findEntityByIdAndGroupId(requestId, groupId);
        EventGroup group = request.getGroup();
        groupAdminPolicy.requireAdmin(group, currentUserId);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request has already been processed");
        }
        long currentMembers = groupMemberRepository.countByGroupId(group.getId());
        groupMembershipPolicy.requireCanAddMember(group, Math.toIntExact(currentMembers));
        groupMembershipPolicy.requireNotMember(
                groupMemberRepository.existsByGroupIdAndUserId(group.getId(), request.getUser().getId()));

        request.setStatus(RequestStatus.ACCEPTED);
        joinRequestRepository.save(request);

        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(request.getUser());
        groupMemberRepository.save(member);

        if (groupCapacityPolicy.refreshStatus(group, Math.toIntExact(currentMembers + 1))) {
            eventGroupRepository.save(group);
        }

        notificationService.create(new NotificationCommand(
                request.getUser(),
                NotificationType.GROUP_INVITATION,
                "Join request approved",
                "Your request to join \"" + group.getTitle() + "\" was approved.",
                NotificationReferenceType.GROUP,
                group.getPublicId()));

        return GroupJoinRequestResponse.from(request);
    }

    @Override
    @Transactional
    public GroupJoinRequestResponse reject(UUID groupId, UUID requestId, Long currentUserId) {
        GroupJoinRequest request = findEntityByIdAndGroupId(requestId, groupId);
        validateGroupAdmin(request.getGroup(), currentUserId);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request has already been processed");
        }

        request.setStatus(RequestStatus.REJECTED);
        GroupJoinRequest saved = joinRequestRepository.save(request);
        notificationService.create(new NotificationCommand(
                request.getUser(),
                NotificationType.GROUP_ACTIVITY,
                "Join request declined",
                "Your request to join \"" + request.getGroup().getTitle() + "\" was declined.",
                NotificationReferenceType.GROUP,
                request.getGroup().getPublicId()));
        return GroupJoinRequestResponse.from(saved);
    }

    @Override
    @Transactional
    public void delete(UUID groupId, UUID requestId, Long currentUserId) {
        GroupJoinRequest request = findEntityByIdAndGroupId(requestId, groupId);
        validateGroupAdminOrRequester(request, currentUserId);
        joinRequestRepository.delete(request);
    }

    private EventGroup findGroupById(UUID groupId) {
        return eventGroupRepository.findByPublicId(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId));
    }

    private GroupJoinRequest findEntityByIdAndGroupId(UUID requestId, UUID groupId) {
        EventGroup group = findGroupById(groupId);
        return joinRequestRepository.findByPublicIdAndGroupId(requestId, group.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Group join request not found with id: " + requestId));
    }

    private void validateGroupAdmin(EventGroup group, Long currentUserId) {
        groupAdminPolicy.requireAdmin(group, currentUserId);
    }

    private void validateGroupAdminOrRequester(GroupJoinRequest request, Long currentUserId) {
        boolean isGroupAdmin = request.getGroup().getAdmin() != null
                && request.getGroup().getAdmin().getId().equals(currentUserId);
        boolean isRequester = request.getUser().getId().equals(currentUserId);

        if (!isGroupAdmin && !isRequester) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access this join request");
        }
    }
}
