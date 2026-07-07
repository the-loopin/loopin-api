package com.loopin.api.controller;

import com.loopin.api.common.security.SecurityUtils;
import com.loopin.api.dto.report.request.CreateReportRequest;
import com.loopin.api.dto.report.response.ReportResponse;
import com.loopin.api.service.abstraction.UserReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class UserReportController {

    private final UserReportService reportService;

    @PostMapping
    public ResponseEntity<ReportResponse> create(@Valid @RequestBody CreateReportRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUser()
                .map(user -> user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.create(currentUserId, request));
    }
}
