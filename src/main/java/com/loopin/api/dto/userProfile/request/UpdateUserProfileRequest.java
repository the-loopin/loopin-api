package com.loopin.api.dto.userProfile.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserProfileRequest {

    @NotBlank(message = "Name field cannot be blank")
    @Size(max = 100, message = "Name can be a maximum of 100 characters")
    private String name;

    @NotBlank(message = "City field cannot be blank")
    @Size(max = 50, message = "City name can be a maximum of 50 characters")
    private String city;

    @Size(max = 500, message = "Bio can be a maximum of 500 characters")
    private String bio;
}