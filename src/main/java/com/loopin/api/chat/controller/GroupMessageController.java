package com.loopin.api.chat.controller;

import com.loopin.api.common.security.SecurityUtils;
import com.loopin.api.chat.dto.request.CreateGroupMessageRequest;
import com.loopin.api.chat.dto.response.GroupMessageResponse;
import com.loopin.api.users.entity.User;
import com.loopin.api.chat.service.GroupMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/v1/groups/{groupId}/messages")
@RequiredArgsConstructor
public class GroupMessageController {

    private final GroupMessageService groupMessageService;

    @GetMapping
    public ResponseEntity<Page<GroupMessageResponse>> getGroupMessages(
            @PathVariable UUID groupId,
            @PageableDefault(page = 0, size = 50) Pageable pageable
    ) {
        Long currentUserId = getCurrentUserId();

        return ResponseEntity.ok(groupMessageService.getGroupMessages(groupId, currentUserId, pageable));
    }

    @PostMapping
    public ResponseEntity<GroupMessageResponse> sendMessage(
            @PathVariable UUID groupId,
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