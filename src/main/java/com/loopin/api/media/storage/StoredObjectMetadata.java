package com.loopin.api.media.storage;

public record StoredObjectMetadata(
    long sizeBytes,
    String contentType,
    String eTag
) {
}
