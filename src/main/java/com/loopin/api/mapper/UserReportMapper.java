package com.loopin.api.mapper;

import com.loopin.api.dto.report.response.ReportResponse;
import com.loopin.api.entity.UserReport;
import org.springframework.stereotype.Component;

@Component
public class UserReportMapper {

    public ReportResponse toResponse(UserReport report) {
        Long targetId = report.getTargetType() == null ? null : switch (report.getTargetType()) {
            case GROUP -> report.getGroup() != null ? report.getGroup().getId() : null;
            case MESSAGE -> report.getMessage() != null ? report.getMessage().getId() : null;
        };

        return new ReportResponse(
                report.getId(),
                report.getReporter().getId(),
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
