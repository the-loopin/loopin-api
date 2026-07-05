package com.loopin.api.dto.report.request;

import com.loopin.api.common.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateReportStatusRequest {

    @NotNull(message = "Status is required")
    private ReportStatus status;
}
