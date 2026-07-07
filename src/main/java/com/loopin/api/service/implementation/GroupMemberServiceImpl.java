package com.loopin.api.service.implementation;

import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.dto.group.request.GroupMemberRequest;
import com.loopin.api.dto.group.response.GroupMemberResponse;
import com.loopin.api.entity.GroupMember;
import com.loopin.api.mapper.GroupMemberMapper;
import com.loopin.api.repository.EventGroupRepository;
import com.loopin.api.repository.GroupMemberRepository;
import com.loopin.api.repository.UserRepository;
import com.loopin.api.service.abstraction.GroupMemberService;
import com.loopin.api.service.abstraction.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupMemberServiceImpl implements GroupMemberService {

    private final GroupService groupService;
    private final GroupMemberRepository groupMemberRepository;
    private final EventGroupRepository eventGroupRepository;
    private final UserRepository userRepository;
    private final GroupMemberMapper groupMemberMapper;

    @Override
    @Transactional
    public GroupMemberResponse addMember(UUID groupId, GroupMemberRequest dto, String currentUsername) {
        groupService.addMember(groupId, dto.getUserId(), currentUsername);
        return getByGroupIdAndUserId(groupId, dto.getUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public GroupMemberResponse getByGroupIdAndUserId(UUID groupId, UUID userId) {
        Long privateGroupId = eventGroupRepository.findByPublicId(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId))
                .getId();
        Long privateUserId = userRepository.findByPublicIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId))
                .getId();

        GroupMember groupMember = groupMemberRepository.findByGroupIdAndUserId(privateGroupId, privateUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Group membership not found for group " + groupId + " and user " + userId));

        return groupMemberMapper.toResponse(groupMember);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupMemberResponse> getByGroupId(UUID groupId) {
        if (!eventGroupRepository.existsByPublicId(groupId)) {
            throw new ResourceNotFoundException("Group not found with id: " + groupId);
        }

        Long privateGroupId = eventGroupRepository.findByPublicId(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId))
                .getId();

        return groupMemberMapper.toResponseList(groupMemberRepository.findByGroupId(privateGroupId));
    }

    @Override
    @Transactional
    public void removeMember(UUID groupId, UUID userId, String currentUsername) {
        groupService.removeMember(groupId, userId, currentUsername);
    }
}
