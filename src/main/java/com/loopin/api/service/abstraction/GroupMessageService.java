package com.loopin.api.service.abstraction;

import com.loopin.api.dto.message.request.CreateGroupMessageRequest;
import com.loopin.api.dto.message.response.GroupMessageResponse;

import java.util.List;

public interface GroupMessageService {

    List<GroupMessageResponse> getGroupMessages(Long groupId, Long currentUserId);

    GroupMessageResponse sendMessage(Long groupId, Long currentUserId, CreateGroupMessageRequest request);
}