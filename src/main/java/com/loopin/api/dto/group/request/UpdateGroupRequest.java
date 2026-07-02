package com.loopin.api.dto.group.request;


import com.loopin.api.common.enums.GroupSizeType;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

//Partial update

@Getter
@Setter
public class UpdateGroupRequest {

    private String title;

    private GroupSizeType groupSize;

    @Min(value = 1, message = "maxMembers must be at least 1")
    private Integer maxMembers;

    private String groupNote;
}