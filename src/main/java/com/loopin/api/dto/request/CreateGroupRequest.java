package com.loopin.api.dto.request;


import com.loopin.api.common.enums.GroupSizeType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateGroupRequest {

    // Optional. If null -> independent group (not tied to an event).
    private Long eventId;

    @NotBlank
    private String title;

    private GroupSizeType groupSize;

    @Min(1)
    private int maxMembers;

    private String groupNote;
}