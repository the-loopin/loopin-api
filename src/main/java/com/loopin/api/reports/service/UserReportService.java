package com.loopin.api.reports.service;

import com.loopin.api.reports.enums.ReportStatus;
import com.loopin.api.reports.dto.request.CreateReportRequest;
import com.loopin.api.reports.dto.response.ReportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserReportService {

    ReportResponse create(Long reporterId, CreateReportRequest request);

    Page<ReportResponse> getReports(ReportStatus status, Pageable pageable);

    ReportResponse updateStatus(UUID reportId, ReportStatus status);
}
