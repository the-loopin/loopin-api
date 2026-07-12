package com.loopin.api.chat.controller;

import com.loopin.api.common.security.CustomUserDetails;
import com.loopin.api.common.metrics.LoopinOperation;
import com.loopin.api.chat.dto.request.CreateGroupMessageRequest;
import com.loopin.api.chat.dto.response.GroupMessageResponse;
import com.loopin.api.chat.service.GroupMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class GroupMessageWebSocketController {

    private final GroupMessageService groupMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/groups/{groupId}/messages")
    @LoopinOperation(domain = "websocket", operation = "message")
    public void sendMessage(
            @DestinationVariable UUID groupId,
            @Valid @Payload CreateGroupMessageRequest request,
            Principal principal
    ) {
        Long currentUserId = getCurrentUserId(principal);
        GroupMessageResponse response = groupMessageService.sendMessage(groupId, currentUserId, request);

        messagingTemplate.convertAndSend("/topic/groups/" + groupId + "/messages", response);
    }

    private Long getCurrentUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken authentication
                && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUser().getId();
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
    }
}
