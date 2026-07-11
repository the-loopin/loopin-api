package com.loopin.api.media.deletemedia;

import com.loopin.api.common.exception.InvalidMediaStateException;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.enums.MediaStatus;
import com.loopin.api.media.shared.access.MediaAccessPolicy;
import com.loopin.api.media.shared.finder.MediaFinder;
import com.loopin.api.media.storage.ObjectStorage;
import com.loopin.api.users.api.UserLookup;
import com.loopin.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeleteMediaHandler {

    private final UserLookup userLookup;
    private final MediaFinder mediaFinder;
    private final MediaAccessPolicy mediaAccessPolicy;
    private final ObjectStorage objectStorage;
    private final DeleteMediaTransaction transaction;

    public void handle(DeleteMediaCommand command) {
        User currentUser = userLookup.findByEmail(command.currentUserEmail());
        MediaAsset media = mediaFinder.findByPublicId(command.mediaId());

        mediaAccessPolicy.requireOwner(media, currentUser);

        if (media.getStatus() == MediaStatus.DELETED) {
            return;
        }

        if (media.getStatus() == MediaStatus.ATTACHED) {
            throw new InvalidMediaStateException(
                "Attached media cannot be deleted"
            );
        }

        objectStorage.delete(media.getObjectKey());

        transaction.markDeleted(
            command.mediaId(),
            currentUser.getId()
        );
    }
}
