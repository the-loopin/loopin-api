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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupMessageServiceImplTest {

    private static final Long GROUP_ID = 1L;
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
                userRepository
        );
    }

    @Test
    void getGroupMessages_ValidMember_ReturnsMessages() {
        EventGroup group = group(GROUP_ID, GroupStatus.OPEN);
        User user = user(USER_ID, "test user");

        when(eventGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupIdAndUserId(GROUP_ID, USER_ID)).thenReturn(true);

        GroupMessage message = message(10L, group, user, "hello");
        when(groupMessageRepository.findByGroupIdOrderByCreatedAtAsc(GROUP_ID)).thenReturn(List.of(message));

        List<GroupMessageResponse> result = groupMessageService.getGroupMessages(GROUP_ID, USER_ID);

        assertEquals(1, result.size());
        assertEquals("hello", result.get(0).getMessageText());
    }

    @Test
    void getGroupMessages_NotMember_ThrowsInvalidGroupStateException() {
        EventGroup group = group(GROUP_ID, GroupStatus.OPEN);

        when(eventGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupIdAndUserId(GROUP_ID, USER_ID)).thenReturn(false);

        assertThrows(InvalidGroupStateException.class, () -> groupMessageService.getGroupMessages(GROUP_ID, USER_ID));
    }

    @Test
    void sendMessage_ValidMember_SavesAndReturnsMessage() {
        EventGroup group = group(GROUP_ID, GroupStatus.OPEN);
        User user = user(USER_ID, "test user");

        when(eventGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupIdAndUserId(GROUP_ID, USER_ID)).thenReturn(true);
        when(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).thenReturn(Optional.of(user));

        GroupMessage savedMessage = message(10L, group, user, "new message");
        when(groupMessageRepository.save(any(GroupMessage.class))).thenReturn(savedMessage);

        CreateGroupMessageRequest request = new CreateGroupMessageRequest();
        request.setMessageText("new message");

        GroupMessageResponse result = groupMessageService.sendMessage(GROUP_ID, USER_ID, request);

        verify(groupMessageRepository).save(any(GroupMessage.class));
        assertEquals("new message", result.getMessageText());
        assertEquals(USER_ID, result.getSenderId());
    }

    @Test
    void sendMessage_GroupArchived_ThrowsInvalidGroupStateException() {
        EventGroup group = group(GROUP_ID, GroupStatus.ARCHIVED);

        when(eventGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));

        CreateGroupMessageRequest request = new CreateGroupMessageRequest();
        request.setMessageText("new message");

        assertThrows(InvalidGroupStateException.class, () -> groupMessageService.sendMessage(GROUP_ID, USER_ID, request));
    }

    @Test
    void sendMessage_NotMember_ThrowsInvalidGroupStateException() {
        EventGroup group = group(GROUP_ID, GroupStatus.OPEN);

        when(eventGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupIdAndUserId(GROUP_ID, USER_ID)).thenReturn(false);

        CreateGroupMessageRequest request = new CreateGroupMessageRequest();
        request.setMessageText("new message");

        assertThrows(InvalidGroupStateException.class, () -> groupMessageService.sendMessage(GROUP_ID, USER_ID, request));
    }

    @Test
    void sendMessage_UserNotFound_ThrowsResourceNotFoundException() {
        EventGroup group = group(GROUP_ID, GroupStatus.OPEN);

        when(eventGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupIdAndUserId(GROUP_ID, USER_ID)).thenReturn(true);
        when(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).thenReturn(Optional.empty());

        CreateGroupMessageRequest request = new CreateGroupMessageRequest();
        request.setMessageText("new message");

        assertThrows(ResourceNotFoundException.class, () -> groupMessageService.sendMessage(GROUP_ID, USER_ID, request));
    }

    private EventGroup group(Long id, GroupStatus status) {
        EventGroup group = new EventGroup();
        group.setId(id);
        group.setStatus(status);
        return group;
    }

    private User user(Long id, String name) {
        User user = new User("test@email.com", name, null);
        user.setId(id);
        return user;
    }

    private GroupMessage message(Long id, EventGroup group, User sender, String text) {
        GroupMessage message = new GroupMessage();
        message.setId(id);
        message.setGroup(group);
        message.setSender(sender);
        message.setMessageText(text);
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }
}
