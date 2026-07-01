package com.loopin.api.dto.request;


import com.loopin.api.common.enums.GroupStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateGroupStatusRequest {

    @NotNull(message = "status is required")
    private GroupStatus status;
}