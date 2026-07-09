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
import com.loopin.api.moderation.ContentModerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupMessageServiceImpl implements GroupMessageService {

    private final GroupMessageRepository groupMessageRepository;
    private final EventGroupRepository eventGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final ContentModerationService contentModerationService;

    @Override
    @Transactional(readOnly = true)
    public Page<GroupMessageResponse> getGroupMessages(UUID groupId, Long currentUserId, Pageable pageable) {
        EventGroup group = findGroupOrThrow(groupId);
        validateGroupAllowsMessaging(group);
        validateUserIsGroupMember(group.getId(), currentUserId);

        return groupMessageRepository.findByGroupIdOrderByCreatedAtAsc(group.getId(), pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public GroupMessageResponse sendMessage(
            UUID groupId,
            Long currentUserId,
            CreateGroupMessageRequest request
    ) {
        EventGroup group = findGroupOrThrow(groupId);
        validateGroupAllowsMessaging(group);
        validateUserIsGroupMember(group.getId(), currentUserId);

        User sender = userRepository.findByIdAndDeletedAtIsNull(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        if (!contentModerationService.moderate(request.getMessageText()).isApproved()) {
            throw new IllegalArgumentException("Content contains blocked language and cannot be sent");
        }

        GroupMessage message = new GroupMessage();
        message.setGroup(group);
        message.setSender(sender);
        message.setMessageText(request.getMessageText().trim());

        GroupMessage savedMessage = groupMessageRepository.save(message);

        return toResponse(savedMessage);
    }

    private EventGroup findGroupOrThrow(UUID groupId) {
        return eventGroupRepository.findByPublicId(groupId)
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
                message.getPublicId(),
                message.getGroup().getPublicId(),
                message.getSender().getPublicId(),
                message.getSender().getName(),
                message.getMessageText(),
                message.getCreatedAt()
        );
    }
}
