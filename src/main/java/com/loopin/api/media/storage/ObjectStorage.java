package com.loopin.api.media.storage;

import java.time.Duration;

public interface ObjectStorage {

    PresignedUpload createUploadUrl(
        String objectKey,
        String contentType,
        Duration expiration
    );

    StoredObjectMetadata getMetadata(String objectKey);

    boolean exists(String objectKey);

    void delete(String objectKey);
}
