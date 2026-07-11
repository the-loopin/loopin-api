package com.loopin.api.media.completeupload;

import com.loopin.api.common.exception.ForbiddenAccessException;
import com.loopin.api.common.exception.InvalidMediaStateException;
import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.enums.MediaStatus;
import com.loopin.api.media.repository.MediaAssetRepository;
import com.loopin.api.media.storage.StoredObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CompleteMediaUploadTransaction {

    private final MediaAssetRepository repository;

    @Transactional
    public MediaAsset complete(
        UUID mediaId,
        Long currentUserId,
        StoredObjectMetadata metadata
    ) {
        MediaAsset media = repository.findByPublicIdForUpdate(mediaId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Media asset was not found"
            ));

        if (!media.getOwner().getId().equals(currentUserId)) {
            throw new ForbiddenAccessException(
                "You do not own this media asset"
            );
        }

        if (media.getStatus() == MediaStatus.UPLOADED
            || media.getStatus() == MediaStatus.ATTACHED) {
            return media;
        }

        if (media.getStatus() != MediaStatus.PENDING_UPLOAD) {
            throw new InvalidMediaStateException(
                "Media cannot be completed from its current status"
            );
        }

        media.markUploaded(metadata);

        return repository.save(media);
    }
}
