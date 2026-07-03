package com.loopin.api.service.implementation;

import com.loopin.api.common.enums.GroupStatus;
import com.loopin.api.common.enums.RequestStatus;
import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.dto.group.request.CreateGroupJoinRequestRequest;
import com.loopin.api.dto.group.response.GroupJoinRequestResponse;
import com.loopin.api.entity.EventGroup;
import com.loopin.api.entity.GroupJoinRequest;
import com.loopin.api.entity.GroupMember;
import com.loopin.api.entity.User;
import com.loopin.api.repository.EventGroupRepository;
import com.loopin.api.repository.GroupJoinRequestRepository;
import com.loopin.api.repository.GroupMemberRepository;
import com.loopin.api.repository.UserRepository;
import com.loopin.api.service.abstraction.GroupJoinRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupJoinRequestServiceImpl implements GroupJoinRequestService {

    private final GroupJoinRequestRepository joinRequestRepository;
    private final EventGroupRepository eventGroupRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Override
    @Transactional
    public GroupJoinRequestResponse create(Long groupId, Long currentUserId, CreateGroupJoinRequestRequest requestDto) {
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
        request.setStatus(RequestStatus.PENDING);

        return GroupJoinRequestResponse.from(joinRequestRepository.save(request));
    }

    @Override
    @Transactional(readOnly = true)
    public GroupJoinRequestResponse getById(Long groupId, Long requestId, Long currentUserId) {
        GroupJoinRequest request = findEntityByIdAndGroupId(requestId, groupId);
        validateGroupAdminOrRequester(request, currentUserId);
        return GroupJoinRequestResponse.from(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupJoinRequestResponse> getByGroupId(Long groupId, Long currentUserId) {
        EventGroup group = findGroupById(groupId);
        validateGroupAdmin(group, currentUserId);

        return joinRequestRepository.findByGroupId(groupId).stream()
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
    public GroupJoinRequestResponse approve(Long groupId, Long requestId, Long currentUserId) {
        GroupJoinRequest request = findEntityByIdAndGroupId(requestId, groupId);
        EventGroup group = request.getGroup();
        validateGroupAdmin(group, currentUserId);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request has already been processed");
        }
        if (group.getStatus() != GroupStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Group is not open");
        }
        if (groupMemberRepository.existsByGroupIdAndUserId(group.getId(), request.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member of this group");
        }

        long currentMembers = groupMemberRepository.countByGroupId(group.getId());
        if (currentMembers >= group.getMaxMembers()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Group is full");
        }

        request.setStatus(RequestStatus.ACCEPTED);
        joinRequestRepository.save(request);

        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(request.getUser());
        groupMemberRepository.save(member);

        if (currentMembers + 1 >= group.getMaxMembers()) {
            group.setStatus(GroupStatus.FULL);
            eventGroupRepository.save(group);
        }

        return GroupJoinRequestResponse.from(request);
    }

    @Override
    @Transactional
    public GroupJoinRequestResponse reject(Long groupId, Long requestId, Long currentUserId) {
        GroupJoinRequest request = findEntityByIdAndGroupId(requestId, groupId);
        validateGroupAdmin(request.getGroup(), currentUserId);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request has already been processed");
        }

        request.setStatus(RequestStatus.REJECTED);
        return GroupJoinRequestResponse.from(joinRequestRepository.save(request));
    }

    @Override
    @Transactional
    public void delete(Long groupId, Long requestId, Long currentUserId) {
        GroupJoinRequest request = findEntityByIdAndGroupId(requestId, groupId);
        validateGroupAdminOrRequester(request, currentUserId);
        joinRequestRepository.delete(request);
    }

    private EventGroup findGroupById(Long groupId) {
        return eventGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId));
    }

    private GroupJoinRequest findEntityByIdAndGroupId(Long requestId, Long groupId) {
        return joinRequestRepository.findByIdAndGroupId(requestId, groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group join request not found with id: " + requestId));
    }

    private void validateGroupAdmin(EventGroup group, Long currentUserId) {
        if (group.getAdmin() == null || !group.getAdmin().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only group admin can manage join requests");
        }
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
