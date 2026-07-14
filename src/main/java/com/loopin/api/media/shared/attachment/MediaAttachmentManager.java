package com.loopin.api.media.shared.attachment;

import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.enums.MediaPurpose;
import com.loopin.api.media.repository.MediaAssetRepository;
import com.loopin.api.media.shared.cleanup.MediaObjectDeletionRequested;
import com.loopin.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MediaAttachmentManager {

    private final MediaAssetRepository mediaAssetRepository;
    private final MediaAttachmentPolicy attachmentPolicy;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.MANDATORY)
    public MediaAsset attach(
        UUID mediaId,
        User owner,
        MediaPurpose expectedPurpose
    ) {
        if (mediaId == null) {
            return null;
        }

        return attachInternal(
            mediaId,
            owner,
            expectedPurpose
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public MediaAsset replace(
        MediaAsset currentMedia,
        UUID requestedMediaId,
        User newMediaOwner,
        MediaPurpose expectedPurpose
    ) {
        if (requestedMediaId == null) {
            removeInternal(
                currentMedia,
                expectedPurpose
            );

            return null;
        }

        if (currentMedia != null
            && requestedMediaId.equals(currentMedia.getPublicId())) {

            MediaAsset lockedCurrent =
                findForUpdate(currentMedia.getPublicId());

            attachmentPolicy.requireExistingAttachment(
                lockedCurrent,
                expectedPurpose
            );

            return lockedCurrent;
        }

        MediaAsset replacement = attachInternal(
            requestedMediaId,
            newMediaOwner,
            expectedPurpose
        );

        removeInternal(
            currentMedia,
            expectedPurpose
        );

        return replacement;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void remove(
        MediaAsset currentMedia,
        MediaPurpose expectedPurpose
    ) {
        removeInternal(
            currentMedia,
            expectedPurpose
        );
    }

    private MediaAsset attachInternal(
        UUID mediaId,
        User owner,
        MediaPurpose expectedPurpose
    ) {
        MediaAsset media = findForUpdate(mediaId);

        attachmentPolicy.requireNewAttachment(
            media,
            owner,
            expectedPurpose
        );

        media.markAttached();

        return mediaAssetRepository.save(media);
    }

    private void removeInternal(
        MediaAsset currentMedia,
        MediaPurpose expectedPurpose
    ) {
        if (currentMedia == null) {
            return;
        }

        MediaAsset lockedMedia =
            findForUpdate(currentMedia.getPublicId());

        attachmentPolicy.requireExistingAttachment(
            lockedMedia,
            expectedPurpose
        );

        lockedMedia.markDetached();
        lockedMedia.markDeleted();

        mediaAssetRepository.save(lockedMedia);

        eventPublisher.publishEvent(
            new MediaObjectDeletionRequested(
                lockedMedia.getPublicId(),
                lockedMedia.getObjectKey()
            )
        );
    }

    private MediaAsset findForUpdate(UUID mediaId) {
        return mediaAssetRepository
            .findByPublicIdForUpdate(mediaId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Media asset was not found"
            ));
    }
}
