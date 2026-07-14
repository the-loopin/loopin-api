package com.loopin.api.media.mapper;

import com.loopin.api.media.dto.response.MediaReferenceResponse;
import com.loopin.api.media.entity.MediaAsset;
import org.springframework.stereotype.Component;

@Component
public class MediaReferenceMapper {

    public MediaReferenceResponse toResponse(MediaAsset media) {
        if (media == null) {
            return null;
        }

        String contentType =
            media.getVerifiedContentType() != null
                ? media.getVerifiedContentType()
                : media.getDeclaredContentType();

        Long sizeBytes =
            media.getVerifiedFileSize() != null
                ? media.getVerifiedFileSize()
                : media.getDeclaredFileSize();

        return new MediaReferenceResponse(
            media.getPublicId(),
            contentType,
            sizeBytes
        );
    }
}
