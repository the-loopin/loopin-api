package com.loopin.api.service.abstraction;

import com.loopin.api.common.enums.ReportStatus;
import com.loopin.api.dto.report.request.CreateReportRequest;
import com.loopin.api.dto.report.response.ReportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserReportService {

    ReportResponse create(Long reporterId, CreateReportRequest request);

    Page<ReportResponse> getReports(ReportStatus status, Pageable pageable);

    ReportResponse updateStatus(Long reportId, ReportStatus status);
}
