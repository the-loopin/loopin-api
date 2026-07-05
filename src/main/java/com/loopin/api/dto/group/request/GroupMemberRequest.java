package com.loopin.api.dto.group.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class GroupMemberRequest {

    @NotNull
    private UUID userId;
}
