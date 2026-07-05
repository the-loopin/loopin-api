package com.loopin.api.dto.group.request;


import com.loopin.api.common.enums.GroupSizeType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateGroupRequest {

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    @NotBlank
    private String title;

    private GroupSizeType groupSize;

    @Min(1)
    private int maxMembers;

    private String groupNote;
}
