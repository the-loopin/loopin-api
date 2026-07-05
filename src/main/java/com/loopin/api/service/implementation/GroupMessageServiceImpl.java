package com.loopin.api.service.implementation;

import com.loopin.api.common.enums.GroupStatus;
import com.loopin.api.common.exception.InvalidGroupStateException;
import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.dto.message.request.CreateGroupMessageRequest;
import com.loopin.api.dto.message.response.GroupMessageResponse;
import com.loopin.api.entity.EventGroup;
import com.loopin.api.entity.GroupMessage;
import com.loopin.api.entity.User;
import com.loopin.api.repository.EventGroupRepository;
import com.loopin.api.repository.GroupMemberRepository;
import com.loopin.api.repository.GroupMessageRepository;
import com.loopin.api.repository.UserRepository;
import com.loopin.api.service.abstraction.GroupMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupMessageServiceImpl implements GroupMessageService {

    private final GroupMessageRepository groupMessageRepository;
    private final EventGroupRepository eventGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GroupMessageResponse> getGroupMessages(Long groupId, Long currentUserId) {
        EventGroup group = findGroupOrThrow(groupId);
        validateGroupAllowsMessaging(group);
        validateUserIsGroupMember(groupId, currentUserId);

        return groupMessageRepository.findByGroupIdOrderByCreatedAtAsc(groupId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public GroupMessageResponse sendMessage(
            Long groupId,
            Long currentUserId,
            CreateGroupMessageRequest request
    ) {
        EventGroup group = findGroupOrThrow(groupId);
        validateGroupAllowsMessaging(group);
        validateUserIsGroupMember(groupId, currentUserId);

        User sender = userRepository.findByIdAndDeletedAtIsNull(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        GroupMessage message = new GroupMessage();
        message.setGroup(group);
        message.setSender(sender);
        message.setMessageText(request.getMessageText().trim());

        GroupMessage savedMessage = groupMessageRepository.save(message);

        return toResponse(savedMessage);
    }

    private EventGroup findGroupOrThrow(Long groupId) {
        return eventGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));
    }

    private void validateUserIsGroupMember(Long groupId, Long userId) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new InvalidGroupStateException("Only group members can access group messages");
        }
    }

    private void validateGroupAllowsMessaging(EventGroup group) {
        if (group.getStatus() == GroupStatus.ARCHIVED || group.getStatus() == GroupStatus.CANCELLED) {
            throw new InvalidGroupStateException(
                    "Group is " + group.getStatus() + " and no longer allows messaging"
            );
        }
    }

    private GroupMessageResponse toResponse(GroupMessage message) {
        return new GroupMessageResponse(
                message.getId(),
                message.getGroup().getId(),
                message.getSender().getId(),
                message.getSender().getName(),
                message.getMessageText(),
                message.getCreatedAt()
        );
    }
}