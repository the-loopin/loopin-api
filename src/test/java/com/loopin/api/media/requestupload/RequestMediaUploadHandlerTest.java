package com.loopin.api.media.requestupload;

import com.loopin.api.common.exception.MediaStorageException;
import com.loopin.api.media.dto.request.RequestMediaUploadRequest;
import com.loopin.api.media.dto.response.MediaUploadResponse;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.enums.MediaPurpose;
import com.loopin.api.media.enums.MediaStatus;
import com.loopin.api.media.repository.MediaAssetRepository;
import com.loopin.api.media.shared.key.MediaObjectKeyGenerator;
import com.loopin.api.media.shared.policy.MediaUploadPolicy;
import com.loopin.api.media.storage.ObjectStorage;
import com.loopin.api.media.storage.PresignedUpload;
import com.loopin.api.media.storage.StorageProperties;
import com.loopin.api.media.support.MediaTestFixtures;
import com.loopin.api.users.api.UserLookup;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestMediaUploadHandlerTest {

    private UserLookup userLookup;
    private MediaAssetRepository repository;
    private MediaUploadPolicy uploadPolicy;
    private MediaObjectKeyGenerator keyGenerator;
    private ObjectStorage objectStorage;
    private StorageProperties storageProperties;

    private RequestMediaUploadHandler handler;

    @BeforeEach
    void setUp() {
        userLookup = mock(UserLookup.class);
        repository = mock(MediaAssetRepository.class);
        uploadPolicy = mock(MediaUploadPolicy.class);
        keyGenerator = mock(MediaObjectKeyGenerator.class);
        objectStorage = mock(ObjectStorage.class);

        storageProperties = new StorageProperties();
        storageProperties.setUploadUrlTtl(Duration.ofMinutes(10));

        handler = new RequestMediaUploadHandler(
            userLookup,
            repository,
            uploadPolicy,
            keyGenerator,
            objectStorage,
            storageProperties
        );
    }

    @Test
    void handle_ValidRequest_CreatesPendingMediaAndUploadUrl() {
        User owner = MediaTestFixtures.owner();
        RequestMediaUploadRequest request = validRequest();

        String objectKey =
            "media/owner/event_image/generated-media-id";

        Instant expiresAt =
            Instant.parse("2030-01-01T10:10:00Z");

        when(userLookup.findByEmail(
            MediaTestFixtures.OWNER_EMAIL
        )).thenReturn(owner);

        when(keyGenerator.generate(
            eq(owner.getPublicId()),
            eq(MediaPurpose.EVENT_IMAGE),
            any(UUID.class)
        )).thenReturn(objectKey);

        when(objectStorage.createUploadUrl(
            objectKey,
            "image/webp",
            Duration.ofMinutes(10)
        )).thenReturn(
            new PresignedUpload(
                URI.create("https://storage.test/upload"),
                expiresAt
            )
        );

        MediaUploadResponse response = handler.handle(
            new RequestMediaUploadCommand(
                request,
                MediaTestFixtures.OWNER_EMAIL
            )
        );

        ArgumentCaptor<MediaAsset> mediaCaptor =
            ArgumentCaptor.forClass(MediaAsset.class);

        verify(repository).save(mediaCaptor.capture());

        MediaAsset saved = mediaCaptor.getValue();

        assertNotNull(saved.getPublicId());
        assertEquals(owner, saved.getOwner());
        assertEquals(objectKey, saved.getObjectKey());
        assertEquals("event.webp", saved.getOriginalFilename());
        assertEquals("image/webp", saved.getDeclaredContentType());
        assertEquals(1_024L, saved.getDeclaredFileSize());
        assertEquals(MediaPurpose.EVENT_IMAGE, saved.getPurpose());
        assertEquals(MediaStatus.PENDING_UPLOAD, saved.getStatus());

        assertEquals(
            "https://storage.test/upload",
            response.uploadUrl()
        );
        assertEquals(expiresAt, response.expiresAt());

        verify(uploadPolicy).validateRequest(
            MediaPurpose.EVENT_IMAGE,
            "image/webp",
            1_024L
        );
    }

    @Test
    void handle_ContentTypeAndFilename_NormalizesValues() {
        User owner = MediaTestFixtures.owner();
        RequestMediaUploadRequest request = validRequest();

        request.setFileName("  event.webp  ");
        request.setContentType("  IMAGE/WEBP  ");

        when(userLookup.findByEmail(anyString()))
            .thenReturn(owner);

        when(keyGenerator.generate(
            any(),
            any(),
            any()
        )).thenReturn("generated-key");

        when(objectStorage.createUploadUrl(
            anyString(),
            anyString(),
            any()
        )).thenReturn(
            new PresignedUpload(
                URI.create("https://storage.test/upload"),
                Instant.now()
            )
        );

        handler.handle(
            new RequestMediaUploadCommand(
                request,
                MediaTestFixtures.OWNER_EMAIL
            )
        );

        verify(uploadPolicy).validateRequest(
            MediaPurpose.EVENT_IMAGE,
            "image/webp",
            1_024L
        );

        ArgumentCaptor<MediaAsset> mediaCaptor =
            ArgumentCaptor.forClass(MediaAsset.class);

        verify(repository).save(mediaCaptor.capture());

        assertEquals(
            "event.webp",
            mediaCaptor.getValue().getOriginalFilename()
        );

        assertEquals(
            "image/webp",
            mediaCaptor.getValue().getDeclaredContentType()
        );
    }

    @Test
    void handle_InvalidMedia_DoesNotPersistOrCreateUrl() {
        RequestMediaUploadRequest request = validRequest();

        doThrow(new IllegalArgumentException(
            "Unsupported media content type"
        )).when(uploadPolicy).validateRequest(
            any(),
            anyString(),
            anyLong()
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> handler.handle(
                new RequestMediaUploadCommand(
                    request,
                    MediaTestFixtures.OWNER_EMAIL
                )
            )
        );

        verify(repository, never()).save(any());
        verify(keyGenerator, never())
            .generate(any(), any(), any());

        verify(objectStorage, never())
            .createUploadUrl(
                anyString(),
                anyString(),
                any()
            );
    }

    @Test
    void handle_StorageFailure_PropagatesException() {
        User owner = MediaTestFixtures.owner();

        when(userLookup.findByEmail(anyString()))
            .thenReturn(owner);

        when(keyGenerator.generate(
            any(),
            any(),
            any()
        )).thenReturn("generated-key");

        when(objectStorage.createUploadUrl(
            anyString(),
            anyString(),
            any()
        )).thenThrow(
            new MediaStorageException(
                "Storage unavailable"
            )
        );

        assertThrows(
            MediaStorageException.class,
            () -> handler.handle(
                new RequestMediaUploadCommand(
                    validRequest(),
                    MediaTestFixtures.OWNER_EMAIL
                )
            )
        );
    }

    private RequestMediaUploadRequest validRequest() {
        RequestMediaUploadRequest request =
            new RequestMediaUploadRequest();

        request.setPurpose(MediaPurpose.EVENT_IMAGE);
        request.setFileName("event.webp");
        request.setContentType("image/webp");
        request.setSizeBytes(1_024L);

        return request;
    }
}
