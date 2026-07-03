package com.loopin.api.dto.group.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupMemberRequest {

    @NotNull
    private Long userId;
}
