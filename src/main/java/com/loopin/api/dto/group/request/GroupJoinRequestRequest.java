package com.loopin.api.dto.group.request;


import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupJoinRequestRequest {

    @NotNull
    private Long groupId;

    @NotNull
    private Long userId;

    private String message;
}