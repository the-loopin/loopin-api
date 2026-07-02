package com.loopin.api.service.implementation;

import com.loopin.api.common.exception.DuplicateResourceException;
import com.loopin.api.common.exception.InvalidGroupStateException;
import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.dto.group.request.GroupMemberRequest;
import com.loopin.api.dto.group.response.GroupMemberResponse;
import com.loopin.api.entity.EventGroup;
import com.loopin.api.entity.GroupMember;
import com.loopin.api.entity.User;
import com.loopin.api.mapper.GroupMemberMapper;
import com.loopin.api.repository.EventGroupRepository;
import com.loopin.api.repository.GroupMemberRepository;
import com.loopin.api.repository.UserRepository;
import com.loopin.api.service.abstraction.GroupMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupMemberServiceImpl implements GroupMemberService {

    private final GroupMemberRepository groupMemberRepository;
    private final EventGroupRepository eventGroupRepository;
    private final UserRepository userRepository;
    private final GroupMemberMapper groupMemberMapper;


    @Override
    @Transactional
    public GroupMemberResponse addMember(GroupMemberRequest dto) {
        EventGroup group = eventGroupRepository.findById(dto.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + dto.getGroupId()));
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + dto.getUserId()));

        if (groupMemberRepository.existsByGroup_IdAndUser_Id(group.getId(), user.getId())) {
            throw new DuplicateResourceException("Group member already exists: " + dto.getGroupId());
        }

        long currentMembers = groupMemberRepository.countByGroup_Id(group.getId());
        if (currentMembers >= group.getMaxMembers()) {
            throw  new InvalidGroupStateException("Group member is too large");
        }

        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);

        return GroupMemberResponse.from(groupMemberRepository.save(member));
    }

    @Override
    @Transactional(readOnly = true)
    public GroupMemberResponse getById(Long id) {

        GroupMember groupMember = groupMemberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group member not found with id: " + id));

        return GroupMemberResponse.from(groupMember);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupMemberResponse> getByGroupId(Long groupId) {
        if (!eventGroupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Group not found with id: " + groupId);
        }

        List<GroupMember> members = groupMemberRepository.findByGroup_Id(groupId);

        return groupMemberMapper.toResponseList(members);
    }


    @Override
    @Transactional
    public void removeMember(Long groupId, Long userId) {
        if (!groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, userId)) {
            throw new ResourceNotFoundException("User or group not found");
        }
        groupMemberRepository.deleteByGroup_IdAndUser_Id(groupId, userId);
    }


    private GroupMember findEntityById(Long id) {
        return groupMemberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + id));
    }
}