package com.loopin.api.media.storage.s3;

import com.loopin.api.common.exception.StoredObjectNotFoundException;
import com.loopin.api.media.storage.PresignedUpload;
import com.loopin.api.media.storage.StoredObjectMetadata;
import com.loopin.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class S3ObjectStorageIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private S3ObjectStorage objectStorage;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void presignedUploadMetadataAndDelete_WorkEndToEnd() throws Exception {
        String objectKey = "media/test/event-image/" + UUID.randomUUID();
        byte[] content = "fake-webp-test-content".getBytes(StandardCharsets.UTF_8);

        PresignedUpload upload = objectStorage.createUploadUrl(
                objectKey, "image/webp", Duration.ofMinutes(5)
        );
        assertNotNull(upload.uploadUrl());
        assertNotNull(upload.expiresAt());

        HttpResponse<Void> putResponse = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(upload.uploadUrl())
                        .header("Content-Type", "image/webp")
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(content))
                        .build(),
                HttpResponse.BodyHandlers.discarding()
        );
        assertTrue(putResponse.statusCode() >= 200 && putResponse.statusCode() < 300);
        assertTrue(objectStorage.exists(objectKey));

        StoredObjectMetadata metadata = objectStorage.getMetadata(objectKey);
        assertEquals(content.length, metadata.sizeBytes());
        assertEquals("image/webp", metadata.contentType());
        assertNotNull(metadata.eTag());

        objectStorage.delete(objectKey);
        assertFalse(objectStorage.exists(objectKey));
    }

    @Test
    void getMetadata_MissingObject_ThrowsExpectedException() {
        assertThrows(StoredObjectNotFoundException.class, () ->
                objectStorage.getMetadata("media/test/missing/" + UUID.randomUUID())
        );
    }

    @Test
    void delete_MissingObject_IsIdempotent() {
        String missingKey = "media/test/already-missing/" + UUID.randomUUID();
        assertDoesNotThrow(() -> objectStorage.delete(missingKey));
        assertDoesNotThrow(() -> objectStorage.delete(missingKey));
    }

    @Test
    void presignedUpload_WithDifferentContentType_IsRejected() throws Exception {
        String objectKey = "media/test/wrong-content-type/" + UUID.randomUUID();
        PresignedUpload upload = objectStorage.createUploadUrl(
                objectKey, "image/webp", Duration.ofMinutes(5)
        );

        HttpResponse<Void> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(upload.uploadUrl())
                        .header("Content-Type", "image/png")
                        .PUT(HttpRequest.BodyPublishers.ofByteArray("test".getBytes(StandardCharsets.UTF_8)))
                        .build(),
                HttpResponse.BodyHandlers.discarding()
        );

        assertTrue(response.statusCode() >= 400);
        assertFalse(objectStorage.exists(objectKey));
    }
}
