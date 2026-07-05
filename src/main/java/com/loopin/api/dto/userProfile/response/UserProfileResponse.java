package com.loopin.api.dto.userProfile.response;

import com.loopin.api.dto.interest.InterestResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private UUID id;
    private String username;
    private String email;
    private String name;
    private String city;
    private String bio;
    private List<InterestResponse> interests;
}
