package com.loopin.api.groups.controller;

import com.loopin.api.common.security.SecurityUtils;
import com.loopin.api.groups.dto.response.GroupResponse;
import com.loopin.api.groups.listmygroups.ListMyGroupsHandler;
import com.loopin.api.groups.listmygroups.ListMyGroupsQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/me/groups")
@RequiredArgsConstructor
public class MyGroupsController {

    private final ListMyGroupsHandler handler;

    @GetMapping
    public ResponseEntity<List<GroupResponse>>
    getMyGroups() {
        String currentUsername =
            SecurityUtils
                .getRequiredCurrentUserEmail();

        return ResponseEntity.ok(
            handler.handle(
                new ListMyGroupsQuery(
                    currentUsername
                )
            )
        );
    }
}
