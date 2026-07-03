package com.loopin.api.service.implementation;


import com.loopin.api.dto.group.request.CreateGroupRequest;
import com.loopin.api.mapper.GroupMapper;
import com.loopin.api.dto.group.request.UpdateGroupRequest;
import com.loopin.api.dto.group.request.UpdateGroupStatusRequest;
import com.loopin.api.dto.group.response.GroupResponse;
import com.loopin.api.entity.Event;
import com.loopin.api.entity.EventGroup;
import com.loopin.api.entity.GroupMember;
import com.loopin.api.entity.User;
import com.loopin.api.common.enums.GroupStatus;
import com.loopin.api.common.enums.GroupSizeType;
import com.loopin.api.common.exception.InvalidGroupStateException;
import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.repository.EventGroupRepository;
import com.loopin.api.repository.EventRepository;
import com.loopin.api.repository.GroupMemberRepository;
import com.loopin.api.repository.UserRepository;
import com.loopin.api.service.abstraction.GroupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupServiceImpl implements GroupService {

    private final EventGroupRepository eventGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupMapper  groupMapper;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public GroupServiceImpl(EventGroupRepository eventGroupRepository,
                            GroupMapper groupMapper,
                            GroupMemberRepository groupMemberRepository,
                            EventRepository eventRepository,
                            UserRepository userRepository) {
        this.eventGroupRepository = eventGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.groupMapper = groupMapper;
    }

    @Override
    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request, String currentUsername) {
        User currentUser = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));

        // Event is optional: null eventId => independent group
        Event event = null;
        if (request.getEventId() != null) {
            event = eventRepository.findById(request.getEventId())
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + request.getEventId()));
        }

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

    public GroupResponse getGroup(Long groupId) {
        EventGroup group = findGroupOrThrow(groupId);
        GroupResponse response=groupMapper.toGroupResponse(group);
        return response;
    }

    @Override
    @Transactional
    public GroupResponse updateGroup(Long groupId, UpdateGroupRequest request, String currentUsername) {
        EventGroup group = findGroupOrThrow(groupId);
        validateGroupAcceptsMembershipChanges(group);

        if (!group.getAdmin().getEmail().equals(currentUsername)) {
            throw new InvalidGroupStateException("Only the group admin can update this group");
        }

        int currentMemberCount = groupMemberRepository.countByGroupId(groupId);

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
    public void addMember(Long groupId, Long userId) {
        EventGroup group = findGroupOrThrow(groupId);
        applyCapacityFromGroupSize(group);
        int currentMemberCount = groupMemberRepository.countByGroupId(groupId);
        refreshGroupCapacityStatus(group, currentMemberCount);
        validateGroupAcceptsNewMembers(group);

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
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
    }

    @Override
    @Transactional
    public void removeMember(Long groupId, Long userId) {
        EventGroup group = findGroupOrThrow(groupId);
        validateGroupAcceptsMembershipChanges(group);

        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));

        int currentMemberCount = groupMemberRepository.countByGroupId(groupId);
        groupMemberRepository.delete(member);
        refreshGroupCapacityStatus(group, currentMemberCount - 1);
    }

    @Override
    @Transactional
    public GroupResponse updateGroupStatus(
            Long groupId,
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

    private EventGroup findGroupOrThrow(Long groupId) {
        return eventGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));
    }

}
