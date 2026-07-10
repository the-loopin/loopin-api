package com.loopin.api.core.interests.mapper;

import com.loopin.api.core.interests.dto.InterestResponse;
import com.loopin.api.core.interests.entity.Interest;
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
