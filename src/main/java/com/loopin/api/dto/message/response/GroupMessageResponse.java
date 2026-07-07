package com.loopin.api.dto.message.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class GroupMessageResponse {

    private Long id;
    private Long groupId;
    private Long senderId;
    private String senderName;
    private String messageText;
    private LocalDateTime createdAt;
}