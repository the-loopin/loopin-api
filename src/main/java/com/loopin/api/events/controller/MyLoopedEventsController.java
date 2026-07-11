package com.loopin.api.events.controller;

import com.loopin.api.common.security.SecurityUtils;
import com.loopin.api.events.dto.response.LoopedEventResponse;
import com.loopin.api.events.listmyloopedevents.ListMyLoopedEventsHandler;
import com.loopin.api.events.listmyloopedevents.ListMyLoopedEventsQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/me/looped-events")
@RequiredArgsConstructor
public class MyLoopedEventsController {

    private final ListMyLoopedEventsHandler handler;

    @GetMapping
    @Operation(
        summary = "Get my looped events",
        description = "Retrieve events looped into by the current user."
    )
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<Page<LoopedEventResponse>> getMyLoopedEvents(
        @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(
            handler.handle(
                new ListMyLoopedEventsQuery(
                    SecurityUtils
                        .getRequiredCurrentUserEmail(),
                    pageable
                )
            )
        );
    }
}
