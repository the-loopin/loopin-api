package com.loopin.api.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class GroupMessageResponse {

    private UUID id;
    private UUID groupId;
    private UUID senderId;
    private String senderName;
    private String messageText;
    private LocalDateTime createdAt;
}