package com.loopin.api.media.deletemedia;

import com.loopin.api.common.exception.ForbiddenAccessException;
import com.loopin.api.common.exception.InvalidMediaStateException;
import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.enums.MediaStatus;
import com.loopin.api.media.repository.MediaAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeleteMediaTransaction {

    private final MediaAssetRepository mediaAssetRepository;

    @Transactional
    public void markDeleted(
        UUID mediaId,
        Long currentUserId
    ) {
        MediaAsset media = mediaAssetRepository
            .findByPublicIdForUpdate(mediaId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Media asset was not found"
            ));

        if (!media.getOwner().getId().equals(currentUserId)) {
            throw new ForbiddenAccessException(
                "You do not own this media asset"
            );
        }

        // Idempotent behavior: do nothing if already deleted
        if (media.getStatus() == MediaStatus.DELETED) {
            return;
        }

        if (media.getStatus() == MediaStatus.ATTACHED) {
            throw new InvalidMediaStateException(
                "Attached media cannot be deleted"
            );
        }

        media.markDeleted();

        mediaAssetRepository.save(media);
    }
}
