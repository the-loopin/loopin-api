package com.loopin.api.chat.service;

import com.loopin.api.groups.enums.GroupStatus;
import com.loopin.api.common.exception.InvalidGroupStateException;
import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.chat.dto.request.CreateGroupMessageRequest;
import com.loopin.api.chat.dto.response.GroupMessageResponse;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.chat.entity.GroupMessage;
import com.loopin.api.users.entity.User;
import com.loopin.api.groups.repository.EventGroupRepository;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.chat.repository.GroupMessageRepository;
import com.loopin.api.users.repository.UserRepository;
import com.loopin.api.moderation.ContentModerationProperties;
import com.loopin.api.moderation.ContentModerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupMessageServiceImplTest {

    private static final Long GROUP_ID = 1L;
    private static final UUID GROUP_UUID = UUID.randomUUID();
    private static final Long USER_ID = 2L;

    private GroupMessageRepository groupMessageRepository;
    private EventGroupRepository eventGroupRepository;
    private GroupMemberRepository groupMemberRepository;
    private UserRepository userRepository;

    private GroupMessageServiceImpl groupMessageService;

    @BeforeEach
    void setUp() {
        groupMessageRepository = mock(GroupMessageRepository.class);
        eventGroupRepository = mock(EventGroupRepository.class);
        groupMemberRepository = mock(GroupMemberRepository.class);
        userRepository = mock(UserRepository.class);

        groupMessageService = new GroupMessageServiceImpl(
                groupMessageRepository,
                eventGroupRepository,
                groupMemberRepository,
                userRepository,
                new ContentModerationService(new ContentModerationProperties())
        );
    }

    @Test
    void getGroupMessages_ValidMember_ReturnsMessages() {
        EventGroup group = group(GROUP_ID, GROUP_UUID, GroupStatus.OPEN);
        User user = user(USER_ID, "test user");

        when(eventGroupRepository.findByPublicId(GROUP_UUID)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupIdAndUserId(GROUP_ID, USER_ID)).thenReturn(true);

        GroupMessage message = message(10L, group, user, "hello");
        Pageable pageable = PageRequest.of(0, 50);
        when(groupMessageRepository.findByGroupIdOrderByCreatedAtAsc(GROUP_ID, pageable)).thenReturn(new PageImpl<>(List.of(message)));

        Page<GroupMessageResponse> result = groupMessageService.getGroupMessages(GROUP_UUID, USER_ID, pageable);

        assertEquals(1, result.getContent().size());
        assertEquals("hello", result.getContent().get(0).getMessageText());
    }

    @Test
    void getGroupMessages_NotMember_ThrowsInvalidGroupStateException() {
        EventGroup group = group(GROUP_ID, GROUP_UUID, GroupStatus.OPEN);

        when(eventGroupRepository.findByPublicId(GROUP_UUID)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupIdAndUserId(GROUP_ID, USER_ID)).thenReturn(false);

        Pageable pageable = PageRequest.of(0, 50);
        assertThrows(InvalidGroupStateException.class, () -> groupMessageService.getGroupMessages(GROUP_UUID, USER_ID, pageable));
    }

    @Test
    void sendMessage_ValidMember_SavesAndReturnsMessage() {
        EventGroup group = group(GROUP_ID, GROUP_UUID, GroupStatus.OPEN);
        User user = user(USER_ID, "test user");

        when(eventGroupRepository.findByPublicId(GROUP_UUID)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupIdAndUserId(GROUP_ID, USER_ID)).thenReturn(true);
        when(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).thenReturn(Optional.of(user));

        GroupMessage savedMessage = message(10L, group, user, "new message");
        when(groupMessageRepository.save(any(GroupMessage.class))).thenReturn(savedMessage);

        CreateGroupMessageRequest request = new CreateGroupMessageRequest();
        request.setMessageText("new message");

        GroupMessageResponse result = groupMessageService.sendMessage(GROUP_UUID, USER_ID, request);

        verify(groupMessageRepository).save(any(GroupMessage.class));
        assertEquals("new message", result.getMessageText());
        assertEquals(user.getPublicId(), result.getSenderId());
    }

    @Test
    void sendMessage_GroupArchived_ThrowsInvalidGroupStateException() {
        EventGroup group = group(GROUP_ID, GROUP_UUID, GroupStatus.ARCHIVED);

        when(eventGroupRepository.findByPublicId(GROUP_UUID)).thenReturn(Optional.of(group));

        CreateGroupMessageRequest request = new CreateGroupMessageRequest();
        request.setMessageText("new message");

        assertThrows(InvalidGroupStateException.class, () -> groupMessageService.sendMessage(GROUP_UUID, USER_ID, request));
    }

    @Test
    void sendMessage_NotMember_ThrowsInvalidGroupStateException() {
        EventGroup group = group(GROUP_ID, GROUP_UUID, GroupStatus.OPEN);

        when(eventGroupRepository.findByPublicId(GROUP_UUID)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupIdAndUserId(GROUP_ID, USER_ID)).thenReturn(false);

        CreateGroupMessageRequest request = new CreateGroupMessageRequest();
        request.setMessageText("new message");

        assertThrows(InvalidGroupStateException.class, () -> groupMessageService.sendMessage(GROUP_UUID, USER_ID, request));
    }

    @Test
    void sendMessage_UserNotFound_ThrowsResourceNotFoundException() {
        EventGroup group = group(GROUP_ID, GROUP_UUID, GroupStatus.OPEN);

        when(eventGroupRepository.findByPublicId(GROUP_UUID)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupIdAndUserId(GROUP_ID, USER_ID)).thenReturn(true);
        when(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).thenReturn(Optional.empty());

        CreateGroupMessageRequest request = new CreateGroupMessageRequest();
        request.setMessageText("new message");

        assertThrows(ResourceNotFoundException.class, () -> groupMessageService.sendMessage(GROUP_UUID, USER_ID, request));
    }

    @Test
    void sendMessage_BlockedText_IsRejectedBeforePersistence() {
        EventGroup group = group(GROUP_ID, GROUP_UUID, GroupStatus.OPEN);
        User user = user(USER_ID, "test user");
        ContentModerationProperties properties = new ContentModerationProperties();
        properties.setBannedWords(List.of("scam"));
        groupMessageService = new GroupMessageServiceImpl(
                groupMessageRepository, eventGroupRepository, groupMemberRepository, userRepository,
                new ContentModerationService(properties));

        when(eventGroupRepository.findByPublicId(GROUP_UUID)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupIdAndUserId(GROUP_ID, USER_ID)).thenReturn(true);
        when(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).thenReturn(Optional.of(user));

        CreateGroupMessageRequest request = new CreateGroupMessageRequest();
        request.setMessageText("Known SCAM link");

        assertThrows(IllegalArgumentException.class, () -> groupMessageService.sendMessage(GROUP_UUID, USER_ID, request));
        verify(groupMessageRepository, never()).save(any(GroupMessage.class));
    }

    private EventGroup group(Long id, UUID publicId, GroupStatus status) {
        EventGroup group = new EventGroup();
        group.setId(id);
        group.setPublicId(publicId);
        group.setStatus(status);
        return group;
    }

    private User user(Long id, String name) {
        User user = new User("test@email.com", name, null);
        user.setId(id);
        user.setPublicId(UUID.randomUUID());
        return user;
    }

    private GroupMessage message(Long id, EventGroup group, User sender, String text) {
        GroupMessage message = new GroupMessage();
        message.setId(id);
        message.setPublicId(UUID.randomUUID());
        message.setGroup(group);
        message.setSender(sender);
        message.setMessageText(text);
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }
}
