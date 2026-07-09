package com.loopin.api.reports.controller;

import com.loopin.api.reports.enums.ReportStatus;
import com.loopin.api.reports.dto.request.UpdateReportStatusRequest;
import com.loopin.api.reports.dto.response.ReportResponse;
import com.loopin.api.reports.service.UserReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminReportController {

    private final UserReportService reportService;

    @GetMapping
    public ResponseEntity<Page<ReportResponse>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(reportService.getReports(status, pageable));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ReportResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReportStatusRequest request
    ) {
        return ResponseEntity.ok(reportService.updateStatus(id, request.getStatus()));
    }
}
