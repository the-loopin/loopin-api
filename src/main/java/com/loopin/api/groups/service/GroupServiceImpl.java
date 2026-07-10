package com.loopin.api.groups.service;


import com.loopin.api.groups.dto.request.CreateGroupRequest;
import com.loopin.api.groups.mapper.GroupMapper;
import com.loopin.api.groups.dto.request.UpdateGroupRequest;
import com.loopin.api.groups.dto.request.UpdateGroupStatusRequest;
import com.loopin.api.groups.dto.response.GroupResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.entity.EventGroup;
import com.loopin.api.groups.entity.GroupMember;
import com.loopin.api.users.entity.User;
import com.loopin.api.groups.enums.GroupStatus;
import com.loopin.api.groups.enums.GroupSizeType;
import com.loopin.api.notifications.enums.NotificationReferenceType;
import com.loopin.api.notifications.enums.NotificationType;
import com.loopin.api.common.exception.InvalidGroupStateException;
import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.events.repository.EventGroupRepository;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.users.repository.UserRepository;
import com.loopin.api.groups.service.GroupService;
import com.loopin.api.notifications.service.NotificationService;
import com.loopin.api.notifications.service.NotificationCommand;
import com.loopin.api.moderation.ContentModerationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GroupServiceImpl implements GroupService {

    private final EventGroupRepository eventGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupMapper  groupMapper;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ContentModerationService contentModerationService;

    public GroupServiceImpl(EventGroupRepository eventGroupRepository,
                            GroupMapper groupMapper,
                            GroupMemberRepository groupMemberRepository,
                            EventRepository eventRepository,
                            UserRepository userRepository,
                            NotificationService notificationService,
                            ContentModerationService contentModerationService) {
        this.eventGroupRepository = eventGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.contentModerationService = contentModerationService;
        this.groupMapper = groupMapper;
    }

    @Override
    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request, String currentUsername) {
        rejectUnsafeContent(request.getTitle(), request.getGroupNote());
        User currentUser = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));

        Event event = eventRepository.findByPublicIdAndDeletedAtIsNull(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + request.getEventId()));

        EventGroup group = groupMapper.toEntity(request,currentUser,event);
        applyCapacityFromGroupSize(group);

        EventGroup savedGroup = eventGroupRepository.save(group);

        // The creator automatically becomes the group admin (set above) and a member
        GroupMember creatorMembership = new GroupMember();
        creatorMembership.setGroup(savedGroup);
        creatorMembership.setUser(currentUser);
        groupMemberRepository.save(creatorMembership);
        refreshGroupCapacityStatus(savedGroup, 1);

        GroupResponse response=groupMapper.toGroupResponse(savedGroup);
        return response;
    }

    public GroupResponse getGroup(UUID groupId) {
        EventGroup group = findGroupOrThrow(groupId);
        GroupResponse response=groupMapper.toGroupResponse(group);
        return response;
    }

    @Override
    @Transactional
    public GroupResponse updateGroup(UUID groupId, UpdateGroupRequest request, String currentUsername) {
        EventGroup group = findGroupOrThrow(groupId);
        validateGroupAcceptsMembershipChanges(group);

        if (!group.getAdmin().getEmail().equals(currentUsername)) {
            throw new InvalidGroupStateException("Only the group admin can update this group");
        }

        rejectUnsafeContent(request.getTitle(), request.getGroupNote());

        int currentMemberCount = groupMemberRepository.countByGroupId(group.getId());

        if (request.getTitle() != null) {
            group.setTitle(request.getTitle());
        }

        if (request.getGroupSize() != null) {
            group.setGroupSize(request.getGroupSize());
        }

        applyCapacityFromGroupSize(group);

        if (group.getMaxMembers() < currentMemberCount) {
            throw new InvalidGroupStateException(
                    "Max members cannot be less than the current member count: " + currentMemberCount);
        }

        if (request.getGroupNote() != null) {
            group.setGroupNote(request.getGroupNote());
        }

        refreshGroupCapacityStatus(group, currentMemberCount);
        EventGroup updatedGroup = eventGroupRepository.save(group);

        return groupMapper.toGroupResponse(updatedGroup);
    }

    @Override
    @Transactional
    public void addMember(UUID groupId, UUID userId, String currentUsername) {
        EventGroup group = findGroupOrThrow(groupId);
        validateGroupAdmin(group, currentUsername);
        applyCapacityFromGroupSize(group);
        int currentMemberCount = groupMemberRepository.countByGroupId(group.getId());
        refreshGroupCapacityStatus(group, currentMemberCount);
        validateGroupAcceptsNewMembers(group);

        User user = userRepository.findByPublicIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (groupMemberRepository.existsByGroupIdAndUserId(group.getId(), user.getId())) {
            throw new InvalidGroupStateException("User is already a member of this group");
        }

        if (currentMemberCount >= group.getMaxMembers()) {
            refreshGroupCapacityStatus(group, currentMemberCount);
            throw new InvalidGroupStateException("Group has reached its maximum number of members");
        }

        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);
        groupMemberRepository.save(member);
        refreshGroupCapacityStatus(group, currentMemberCount + 1);
        notificationService.create(new NotificationCommand(
                user,
                NotificationType.GROUP_INVITATION,
                "Added to group",
                "You were added to \"" + group.getTitle() + "\".",
                NotificationReferenceType.GROUP,
                group.getPublicId()));
    }

    @Override
    @Transactional
    public void removeMember(UUID groupId, UUID userId, String currentUsername) {
        EventGroup group = findGroupOrThrow(groupId);
        validateGroupAdmin(group, currentUsername);
        validateGroupAcceptsMembershipChanges(group);

        User user = userRepository.findByPublicIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(group.getId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));

        int currentMemberCount = groupMemberRepository.countByGroupId(group.getId());
        groupMemberRepository.delete(member);
        refreshGroupCapacityStatus(group, currentMemberCount - 1);
        notificationService.create(new NotificationCommand(
                user,
                NotificationType.GROUP_ACTIVITY,
                "Removed from group",
                "You were removed from \"" + group.getTitle() + "\".",
                NotificationReferenceType.GROUP,
                group.getPublicId()));
    }

    @Override
    @Transactional
    public GroupResponse updateGroupStatus(
            UUID groupId,
            UpdateGroupStatusRequest request,
            String currentUsername) {

        EventGroup group = findGroupOrThrow(groupId);

        if (!group.getAdmin().getEmail().equals(currentUsername)) {
            throw new InvalidGroupStateException(
                    "Only the group admin can update the group status");
        }

        group.setStatus(request.getStatus());

        EventGroup updatedGroup = eventGroupRepository.save(group);

        return groupMapper.toGroupResponse(updatedGroup);
    }

    private void applyCapacityFromGroupSize(EventGroup group) {
        GroupSizeType groupSize = group.getGroupSize();
        if (groupSize == null) {
            throw new InvalidGroupStateException("Group size is required");
        }
        group.setMaxMembers(groupSize.getMaxMembers());
    }

    private void refreshGroupCapacityStatus(EventGroup group, int memberCount) {
        if (group.getStatus() == GroupStatus.ARCHIVED || group.getStatus() == GroupStatus.CANCELLED) {
            return;
        }

        GroupStatus capacityStatus = memberCount >= group.getMaxMembers()
                ? GroupStatus.FULL
                : GroupStatus.OPEN;

        if (group.getStatus() != capacityStatus) {
            group.setStatus(capacityStatus);
            eventGroupRepository.save(group);
        }
    }

    private void validateGroupAcceptsNewMembers(EventGroup group) {
        validateGroupAcceptsMembershipChanges(group);
        if (group.getStatus() == GroupStatus.FULL) {
            throw new InvalidGroupStateException("Group has reached its maximum number of members");
        }
    }

    private void validateGroupAcceptsMembershipChanges(EventGroup group) {
        if (group.getStatus() == GroupStatus.ARCHIVED || group.getStatus() == GroupStatus.CANCELLED) {
            throw new InvalidGroupStateException(
                    "Group is " + group.getStatus() + " and no longer accepts membership updates");
        }
    }

    private void validateGroupAdmin(EventGroup group, String currentUsername) {
        if (currentUsername == null
                || currentUsername.isBlank()
                || group.getAdmin() == null
                || !group.getAdmin().getEmail().equals(currentUsername)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Only the group admin can manage group members");
        }
    }

    private EventGroup findGroupOrThrow(UUID groupId) {
        return eventGroupRepository.findByPublicId(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));
    }

    private void rejectUnsafeContent(String... content) {
        if (!contentModerationService.moderate(content).isApproved()) {
            throw new IllegalArgumentException("Content contains blocked language and cannot be published");
        }
    }

}
