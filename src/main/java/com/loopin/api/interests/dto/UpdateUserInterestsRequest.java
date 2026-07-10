package com.loopin.api.interests.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UpdateUserInterestsRequest {

    @NotNull(message = "Interests are required")
    private List<@Valid UserInterestRequest> interests = new ArrayList<>();
}
