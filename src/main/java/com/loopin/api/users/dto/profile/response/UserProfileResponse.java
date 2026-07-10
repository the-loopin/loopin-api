package com.loopin.api.core.users.dto.profile.response;

import com.loopin.api.core.interests.dto.InterestResponse;
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
