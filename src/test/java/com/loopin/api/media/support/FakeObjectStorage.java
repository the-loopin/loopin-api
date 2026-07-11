package com.loopin.api.media.support;

import com.loopin.api.common.exception.StoredObjectNotFoundException;
import com.loopin.api.media.storage.ObjectStorage;
import com.loopin.api.media.storage.PresignedUpload;
import com.loopin.api.media.storage.StoredObjectMetadata;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FakeObjectStorage implements ObjectStorage {

    private final Map<String, StoredObjectMetadata> objects =
        new ConcurrentHashMap<>();

    @Override
    public PresignedUpload createUploadUrl(
        String objectKey,
        String contentType,
        Duration expiration
    ) {
        return new PresignedUpload(
            URI.create(
                "https://storage.test/upload/"
                    + objectKey
            ),
            Instant.now().plus(expiration)
        );
    }

    @Override
    public StoredObjectMetadata getMetadata(
        String objectKey
    ) {
        StoredObjectMetadata metadata =
            objects.get(objectKey);

        if (metadata == null) {
            throw new StoredObjectNotFoundException();
        }

        return metadata;
    }

    @Override
    public boolean exists(String objectKey) {
        return objects.containsKey(objectKey);
    }

    @Override
    public void delete(String objectKey) {
        objects.remove(objectKey);
    }

    public void put(
        String objectKey,
        StoredObjectMetadata metadata
    ) {
        objects.put(objectKey, metadata);
    }

    public void clear() {
        objects.clear();
    }
}
