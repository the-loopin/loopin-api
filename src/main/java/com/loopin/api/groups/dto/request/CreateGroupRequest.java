package com.loopin.api.groups.dto.request;

import com.loopin.api.groups.enums.GroupSizeType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateGroupRequest {

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    @NotBlank(message = "Group title is required")
    @Size(
        max = 120,
        message = "Group title must not exceed 120 characters"
    )
    private String title;

    private GroupSizeType groupSize;

    @Min(
        value = 1,
        message = "Maximum members must be at least 1"
    )
    private int maxMembers;

    @Size(
        max = 1000,
        message = "Group note must not exceed 1000 characters"
    )
    private String groupNote;

    /**
     * References an uploaded and completed GROUP_IMAGE media asset.
     * The field is optional because groups may be created without an image.
     */
    private UUID imageMediaId;
}
