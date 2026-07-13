package com.loopin.api.media.shared.attachment;

import com.loopin.api.common.exception.InvalidMediaStateException;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.enums.MediaPurpose;
import com.loopin.api.media.enums.MediaStatus;
import com.loopin.api.media.shared.access.MediaAccessPolicy;
import com.loopin.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MediaAttachmentPolicy {

    private final MediaAccessPolicy mediaAccessPolicy;

    public void requireNewAttachment(
        MediaAsset media,
        User expectedOwner,
        MediaPurpose expectedPurpose
    ) {
        mediaAccessPolicy.requireOwner(media, expectedOwner);
        requirePurpose(media, expectedPurpose);

        if (media.getStatus() != MediaStatus.UPLOADED) {
            throw new InvalidMediaStateException(
                "Only uploaded media can be attached"
            );
        }

        if (media.isDeleted()) {
            throw new InvalidMediaStateException(
                "Deleted media cannot be attached"
            );
        }
    }

    public void requireExistingAttachment(
        MediaAsset media,
        MediaPurpose expectedPurpose
    ) {
        requirePurpose(media, expectedPurpose);

        if (media.getStatus() != MediaStatus.ATTACHED) {
            throw new InvalidMediaStateException(
                "Media is not currently attached"
            );
        }

        if (media.isDeleted()) {
            throw new InvalidMediaStateException(
                "Deleted media cannot remain attached"
            );
        }
    }

    private void requirePurpose(
        MediaAsset media,
        MediaPurpose expectedPurpose
    ) {
        if (media.getPurpose() != expectedPurpose) {
            throw new InvalidMediaStateException(
                "Media purpose does not match the target resource"
            );
        }
    }
}
