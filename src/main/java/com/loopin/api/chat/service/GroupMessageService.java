package com.loopin.api.chat.service;

import com.loopin.api.chat.dto.request.CreateGroupMessageRequest;
import com.loopin.api.chat.dto.response.GroupMessageResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface GroupMessageService {

    Page<GroupMessageResponse> getGroupMessages(UUID groupId, Long currentUserId, Pageable pageable);

    GroupMessageResponse sendMessage(UUID groupId, Long currentUserId, CreateGroupMessageRequest request);
}