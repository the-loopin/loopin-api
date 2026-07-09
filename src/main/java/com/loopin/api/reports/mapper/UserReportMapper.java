package com.loopin.api.reports.mapper;

import com.loopin.api.reports.dto.response.ReportResponse;
import com.loopin.api.reports.entity.UserReport;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserReportMapper {

    public ReportResponse toResponse(UserReport report) {
        UUID targetId = report.getTargetType() == null ? null : switch (report.getTargetType()) {
            case GROUP -> report.getGroup() != null ? report.getGroup().getPublicId() : null;
            case MESSAGE -> report.getMessage() != null ? report.getMessage().getPublicId() : null;
        };

        return new ReportResponse(
                report.getPublicId(),
                report.getReporter().getPublicId(),
                report.getTargetType(),
                targetId,
                report.getReason(),
                report.getDetails(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}
