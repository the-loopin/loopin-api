package com.loopin.api.service.implementation;

import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.dto.group.request.GroupMemberRequest;
import com.loopin.api.dto.group.response.GroupMemberResponse;
import com.loopin.api.entity.GroupMember;
import com.loopin.api.mapper.GroupMemberMapper;
import com.loopin.api.repository.EventGroupRepository;
import com.loopin.api.repository.GroupMemberRepository;
import com.loopin.api.service.abstraction.GroupMemberService;
import com.loopin.api.service.abstraction.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupMemberServiceImpl implements GroupMemberService {

    private final GroupService groupService;
    private final GroupMemberRepository groupMemberRepository;
    private final EventGroupRepository eventGroupRepository;
    private final GroupMemberMapper groupMemberMapper;

    @Override
    @Transactional
    public GroupMemberResponse addMember(Long groupId, GroupMemberRequest dto, String currentUsername) {
        groupService.addMember(groupId, dto.getUserId(), currentUsername);
        return getByGroupIdAndUserId(groupId, dto.getUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public GroupMemberResponse getByGroupIdAndUserId(Long groupId, Long userId) {
        GroupMember groupMember = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Group membership not found for group " + groupId + " and user " + userId));

        return groupMemberMapper.toResponse(groupMember);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupMemberResponse> getByGroupId(Long groupId) {
        if (!eventGroupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Group not found with id: " + groupId);
        }

        return groupMemberMapper.toResponseList(groupMemberRepository.findByGroupId(groupId));
    }

    @Override
    @Transactional
    public void removeMember(Long groupId, Long userId, String currentUsername) {
        groupService.removeMember(groupId, userId, currentUsername);
    }
}
