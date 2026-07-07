package com.loopin.api.controller;

import com.loopin.api.common.security.SecurityUtils;
import com.loopin.api.dto.message.request.CreateGroupMessageRequest;
import com.loopin.api.dto.message.response.GroupMessageResponse;
import com.loopin.api.entity.User;
import com.loopin.api.service.abstraction.GroupMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/groups/{groupId}/messages")
@RequiredArgsConstructor
public class GroupMessageController {

    private final GroupMessageService groupMessageService;

    @GetMapping
    public ResponseEntity<List<GroupMessageResponse>> getGroupMessages(
            @PathVariable Long groupId
    ) {
        Long currentUserId = getCurrentUserId();

        return ResponseEntity.ok(groupMessageService.getGroupMessages(groupId, currentUserId));
    }

    @PostMapping
    public ResponseEntity<GroupMessageResponse> sendMessage(
            @PathVariable Long groupId,
            @Valid @RequestBody CreateGroupMessageRequest request
    ) {
        Long currentUserId = getCurrentUserId();

        GroupMessageResponse response = groupMessageService.sendMessage(groupId, currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private Long getCurrentUserId() {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authentication required."
                ));

        return currentUser.getId();
    }
}