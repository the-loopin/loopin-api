package com.loopin.api.media.completeupload;

import com.loopin.api.common.exception.InvalidMediaStateException;
import com.loopin.api.media.dto.response.MediaCompletionResponse;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.enums.MediaStatus;
import com.loopin.api.media.shared.access.MediaAccessPolicy;
import com.loopin.api.media.shared.finder.MediaFinder;
import com.loopin.api.media.shared.validation.StoredMediaValidator;
import com.loopin.api.media.storage.ObjectStorage;
import com.loopin.api.media.storage.StoredObjectMetadata;
import com.loopin.api.users.api.UserLookup;
import com.loopin.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompleteMediaUploadHandler {

    private final UserLookup userLookup;
    private final MediaFinder mediaFinder;
    private final MediaAccessPolicy mediaAccessPolicy;
    private final ObjectStorage objectStorage;
    private final StoredMediaValidator storedMediaValidator;
    private final CompleteMediaUploadTransaction transaction;

    public MediaCompletionResponse handle(
        CompleteMediaUploadCommand command
    ) {
        User currentUser = userLookup.findByEmail(command.currentUserEmail());
        MediaAsset media = mediaFinder.findByPublicId(command.mediaId());

        mediaAccessPolicy.requireOwner(media, currentUser);

        if (media.getStatus() == MediaStatus.UPLOADED
            || media.getStatus() == MediaStatus.ATTACHED) {
            return response(media);
        }

        if (media.getStatus() == MediaStatus.DELETED) {
            throw new InvalidMediaStateException(
                "Deleted media cannot be completed"
            );
        }

        StoredObjectMetadata metadata =
            objectStorage.getMetadata(media.getObjectKey());

        try {
            storedMediaValidator.validate(media, metadata);
        } catch (InvalidMediaStateException exception) {
            objectStorage.delete(media.getObjectKey());
            throw exception;
        }

        MediaAsset completed = transaction.complete(
            command.mediaId(),
            currentUser.getId(),
            metadata
        );

        return response(completed);
    }

    private MediaCompletionResponse response(MediaAsset media) {
        return new MediaCompletionResponse(
            media.getPublicId(),
            media.getStatus()
        );
    }
}
