package com.loopin.api.media.shared.access;

import com.loopin.api.common.exception.ForbiddenAccessException;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.users.entity.User;
import org.springframework.stereotype.Component;

@Component
public class MediaAccessPolicy {

    public void requireOwner(MediaAsset media, User currentUser) {
        if (!media.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenAccessException(
                "You do not own this media asset"
            );
        }
    }
}
