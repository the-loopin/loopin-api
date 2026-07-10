package com.loopin.api.interests.mapper;

import com.loopin.api.interests.dto.InterestResponse;
import com.loopin.api.interests.entity.Interest;
import org.springframework.stereotype.Component;

@Component
public class InterestMapper {

    public InterestResponse toResponse(Interest interest) {
        if (interest == null) {
            return null;
        }

        return new InterestResponse(
                interest.getPublicId(),
                interest.getName(),
                interest.getSlug(),
                interest.getCategory()
        );
    }
}
