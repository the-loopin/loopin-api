package com.loopin.api.media.shared.finder;

import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.repository.MediaAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MediaFinder {

    private final MediaAssetRepository repository;

    public MediaAsset findByPublicId(UUID publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Media asset was not found"
            ));
    }
}
