package com.loopin.api.dto.report.request;

import com.loopin.api.common.enums.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReportRequest {

    @NotNull(message = "Target type is required")
    private ReportTargetType targetType;

    @NotNull(message = "Target id is required")
    private Long targetId;

    @NotBlank(message = "Reason is required")
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;

    @Size(max = 2000, message = "Details must not exceed 2000 characters")
    private String details;
}
