package com.loopin.api.users.dto.profile.response;

import com.loopin.api.interests.dto.InterestResponse;
import com.loopin.api.media.dto.response.MediaReferenceResponse;
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

    private MediaReferenceResponse avatar;

    private List<InterestResponse> interests;

    /**
     * Preserves compatibility with callers that do not provide avatar data.
     */
    public UserProfileResponse(
        UUID id,
        String username,
        String email,
        String name,
        String city,
        String bio,
        List<InterestResponse> interests
    ) {
        this(
            id,
            username,
            email,
            name,
            city,
            bio,
            null,
            interests
        );
    }
}
