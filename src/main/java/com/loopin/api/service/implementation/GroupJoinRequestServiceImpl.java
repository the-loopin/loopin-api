package com.loopin.api.service.implementation;

import com.loopin.api.common.enums.GroupStatus;
import com.loopin.api.common.enums.RequestStatus;
import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.dto.group.request.GroupJoinRequestRequest;
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
    public GroupJoinRequestResponse create(GroupJoinRequestRequest dto) {
        EventGroup group = eventGroupRepository.findById(dto.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + dto.getGroupId()));
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + dto.getUserId()));

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
        request.setMessage(dto.getMessage());
        request.setStatus(RequestStatus.PENDING);

        return GroupJoinRequestResponse.from(joinRequestRepository.save(request));
    }

    @Override
    @Transactional(readOnly = true)
    public GroupJoinRequestResponse getById(Long id) {
        return GroupJoinRequestResponse.from(findEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupJoinRequestResponse> getByGroupId(Long groupId) {
        return joinRequestRepository.findByGroupId(groupId).stream()
                .map(GroupJoinRequestResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupJoinRequestResponse> getByUserId(Long userId) {
        return joinRequestRepository.findByUserId(userId).stream()
                .map(GroupJoinRequestResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public GroupJoinRequestResponse approve(Long id) {
        GroupJoinRequest request = findEntityById(id);
        EventGroup group = request.getGroup();

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request has already been processed");
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
    public GroupJoinRequestResponse reject(Long id) {
        GroupJoinRequest request = findEntityById(id);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request has already been processed");
        }

        request.setStatus(RequestStatus.REJECTED);
        return GroupJoinRequestResponse.from(joinRequestRepository.save(request));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!joinRequestRepository.existsById(id)) {
            throw new ResourceNotFoundException("Group join request not found with id: " + id);
        }
        joinRequestRepository.deleteById(id);
    }

    private GroupJoinRequest findEntityById(Long id) {
        return joinRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group join request not found with id: " + id));
    }
}