package com.loopin.api.dto.group.request;


import com.loopin.api.common.enums.GroupSizeType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateGroupRequest {

    @NotNull(message = "Event ID is required")
    private Long eventId;

    @NotBlank
    private String title;

    private GroupSizeType groupSize;

    @Min(1)
    private int maxMembers;

    private String groupNote;
}
