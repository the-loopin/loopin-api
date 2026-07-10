package com.loopin.api.core.interests.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class UserInterestRequest {

    @NotNull(message = "Interest id is required")
    private UUID interestId;

    @DecimalMin(value = "0.00", message = "Interest weight cannot be negative")
    @DecimalMax(value = "1.00", message = "Interest weight cannot be greater than 1.00")
    private BigDecimal weight;

    @Size(max = 50, message = "Interest source must not exceed 50 characters")
    private String source;
}
