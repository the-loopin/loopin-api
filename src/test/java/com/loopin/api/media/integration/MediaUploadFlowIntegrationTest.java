package com.loopin.api.media.integration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.enums.MediaStatus;
import com.loopin.api.media.repository.MediaAssetRepository;
import com.loopin.api.media.storage.StoredObjectMetadata;
import com.loopin.api.media.support.FakeObjectStorage;
import com.loopin.api.support.AbstractIntegrationTest;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(MediaUploadFlowIntegrationTest.TestConfig.class)
class MediaUploadFlowIntegrationTest
    extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MediaAssetRepository mediaRepository;

    @Autowired
    private FakeObjectStorage objectStorage;

    private User owner;

    @BeforeEach
    void setUp() {
        objectStorage.clear();

        owner = new User(
            "media-flow-owner@example.test",
            "Media Owner",
            null
        );

        owner = userRepository.saveAndFlush(owner);
    }

    @Test
    void fullFlow_RequestCompleteAndDelete()
        throws Exception {

        MvcResult uploadResult = mockMvc.perform(
                post("/v1/media/uploads")
                    .with(user(owner.getEmail())
                        .roles("USER"))
                    .contentType(
                        MediaType.APPLICATION_JSON
                    )
                    .content("""
                                        {
                                          "purpose": "EVENT_IMAGE",
                                          "fileName": "event.webp",
                                          "contentType": "image/webp",
                                          "sizeBytes": 1024
                                        }
                                        """)
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.mediaId").exists())
            .andExpect(jsonPath("$.uploadUrl").exists())
            .andReturn();

        JsonNode uploadJson = jsonMapper.readTree(
            uploadResult
                .getResponse()
                .getContentAsString()
        );

        UUID mediaId = UUID.fromString(
            uploadJson
                .get("mediaId")
                .stringValue()
        );

        MediaAsset pending = mediaRepository
            .findByPublicId(mediaId)
            .orElseThrow();

        assertEquals(
            MediaStatus.PENDING_UPLOAD,
            pending.getStatus()
        );

        objectStorage.put(
            pending.getObjectKey(),
            new StoredObjectMetadata(
                1_024L,
                "image/webp",
                "\"integration-etag\""
            )
        );

        mockMvc.perform(
                post(
                    "/v1/media/uploads/{mediaId}/complete",
                    mediaId
                ).with(user(owner.getEmail())
                    .roles("USER"))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mediaId")
                .value(mediaId.toString()))
            .andExpect(jsonPath("$.status")
                .value("UPLOADED"));

        MediaAsset uploaded = mediaRepository
            .findByPublicId(mediaId)
            .orElseThrow();

        assertEquals(
            MediaStatus.UPLOADED,
            uploaded.getStatus()
        );
        assertEquals(
            1_024L,
            uploaded.getVerifiedFileSize()
        );
        assertEquals(
            "image/webp",
            uploaded.getVerifiedContentType()
        );

        mockMvc.perform(
                delete(
                    "/v1/media/{mediaId}",
                    mediaId
                ).with(user(owner.getEmail())
                    .roles("USER"))
            )
            .andExpect(status().isNoContent());

        MediaAsset deleted = mediaRepository
            .findByPublicId(mediaId)
            .orElseThrow();

        assertEquals(
            MediaStatus.DELETED,
            deleted.getStatus()
        );

        assertFalse(
            objectStorage.exists(
                deleted.getObjectKey()
            )
        );
    }

    @Test
    void completeUpload_SizeMismatch_ReturnsConflict()
        throws Exception {

        UUID mediaId = requestUpload(
            owner,
            "EVENT_IMAGE",
            "event.webp",
            "image/webp",
            1_024L
        );

        MediaAsset media = findMedia(mediaId);

        objectStorage.put(
            media.getObjectKey(),
            new StoredObjectMetadata(
                2_048L,
                "image/webp",
                "\"wrong-size-etag\""
            )
        );

        mockMvc.perform(
                post(
                    "/v1/media/uploads/{mediaId}/complete",
                    mediaId
                )
                    .with(user(owner.getEmail())
                        .roles("USER"))
            )
            .andExpect(status().isConflict());

        assertEquals(
            MediaStatus.PENDING_UPLOAD,
            findMedia(mediaId).getStatus()
        );
    }

    @Test
    void completeUpload_ContentTypeMismatch_ReturnsConflict()
        throws Exception {

        UUID mediaId = requestUpload(
            owner,
            "EVENT_IMAGE",
            "event.webp",
            "image/webp",
            1_024L
        );

        MediaAsset media = findMedia(mediaId);

        objectStorage.put(
            media.getObjectKey(),
            new StoredObjectMetadata(
                1_024L,
                "image/png",
                "\"wrong-content-type-etag\""
            )
        );

        mockMvc.perform(
                post(
                    "/v1/media/uploads/{mediaId}/complete",
                    mediaId
                )
                    .with(user(owner.getEmail())
                        .roles("USER"))
            )
            .andExpect(status().isConflict());

        assertEquals(
            MediaStatus.PENDING_UPLOAD,
            findMedia(mediaId).getStatus()
        );
    }

    @Test
    void completeUpload_ByDifferentUser_ReturnsForbidden()
        throws Exception {

        User otherUser = createOtherUser();

        UUID mediaId = requestUpload(
            owner,
            "EVENT_IMAGE",
            "event.webp",
            "image/webp",
            1_024L
        );

        mockMvc.perform(
                post(
                    "/v1/media/uploads/{mediaId}/complete",
                    mediaId
                )
                    .with(user(otherUser.getEmail())
                        .roles("USER"))
            )
            .andExpect(status().isForbidden());

        assertEquals(
            MediaStatus.PENDING_UPLOAD,
            findMedia(mediaId).getStatus()
        );
    }

    @Test
    void completeUpload_Twice_ReturnsSuccessBothTimes()
        throws Exception {

        UUID mediaId = requestUpload(
            owner,
            "EVENT_IMAGE",
            "event.webp",
            "image/webp",
            1_024L
        );

        MediaAsset media = findMedia(mediaId);

        objectStorage.put(
            media.getObjectKey(),
            new StoredObjectMetadata(
                1_024L,
                "image/webp",
                "\"valid-etag\""
            )
        );

        mockMvc.perform(
                post(
                    "/v1/media/uploads/{mediaId}/complete",
                    mediaId
                )
                    .with(user(owner.getEmail())
                        .roles("USER"))
            )
            .andExpect(status().isOk());

        mockMvc.perform(
                post(
                    "/v1/media/uploads/{mediaId}/complete",
                    mediaId
                )
                    .with(user(owner.getEmail())
                        .roles("USER"))
            )
            .andExpect(status().isOk());

        assertEquals(
            MediaStatus.UPLOADED,
            findMedia(mediaId).getStatus()
        );
    }

    @Test
    void deleteMedia_ByDifferentUser_ReturnsForbidden()
        throws Exception {

        User otherUser = createOtherUser();

        UUID mediaId = requestUpload(
            owner,
            "EVENT_IMAGE",
            "event.webp",
            "image/webp",
            1_024L
        );

        MediaAsset media = findMedia(mediaId);

        objectStorage.put(
            media.getObjectKey(),
            new StoredObjectMetadata(
                1_024L,
                "image/webp",
                "\"valid-etag\""
            )
        );

        mockMvc.perform(
                delete(
                    "/v1/media/{mediaId}",
                    mediaId
                )
                    .with(user(otherUser.getEmail())
                        .roles("USER"))
            )
            .andExpect(status().isForbidden());

        assertEquals(
            MediaStatus.PENDING_UPLOAD,
            findMedia(mediaId).getStatus()
        );

        assertTrue(
            objectStorage.exists(media.getObjectKey())
        );
    }

    @Test
    void deleteMedia_Twice_ReturnsNoContentBothTimes()
        throws Exception {

        UUID mediaId = requestUpload(
            owner,
            "EVENT_IMAGE",
            "event.webp",
            "image/webp",
            1_024L
        );

        MediaAsset media = findMedia(mediaId);

        objectStorage.put(
            media.getObjectKey(),
            new StoredObjectMetadata(
                1_024L,
                "image/webp",
                "\"valid-etag\""
            )
        );

        mockMvc.perform(
                delete(
                    "/v1/media/{mediaId}",
                    mediaId
                )
                    .with(user(owner.getEmail())
                        .roles("USER"))
            )
            .andExpect(status().isNoContent());

        mockMvc.perform(
                delete(
                    "/v1/media/{mediaId}",
                    mediaId
                )
                    .with(user(owner.getEmail())
                        .roles("USER"))
            )
            .andExpect(status().isNoContent());

        assertEquals(
            MediaStatus.DELETED,
            findMedia(mediaId).getStatus()
        );
    }

    @Test
    void deleteAttachedMedia_ReturnsConflict()
        throws Exception {

        UUID mediaId = requestUpload(
            owner,
            "EVENT_IMAGE",
            "event.webp",
            "image/webp",
            1_024L
        );

        MediaAsset media = findMedia(mediaId);

        StoredObjectMetadata metadata =
            new StoredObjectMetadata(
                1_024L,
                "image/webp",
                "\"attached-etag\""
            );

        media.markUploaded(metadata);
        media.markAttached();

        mediaRepository.saveAndFlush(media);

        objectStorage.put(
            media.getObjectKey(),
            metadata
        );

        mockMvc.perform(
                delete(
                    "/v1/media/{mediaId}",
                    mediaId
                )
                    .with(user(owner.getEmail())
                        .roles("USER"))
            )
            .andExpect(status().isConflict());

        assertEquals(
            MediaStatus.ATTACHED,
            findMedia(mediaId).getStatus()
        );

        assertTrue(
            objectStorage.exists(media.getObjectKey())
        );
    }

    @Test
    void completeUpload_MissingObject_ReturnsConflict()
        throws Exception {

        UUID mediaId = requestUpload(
            owner,
            "EVENT_IMAGE",
            "event.webp",
            "image/webp",
            1_024L
        );

        mockMvc.perform(
                post(
                    "/v1/media/uploads/{mediaId}/complete",
                    mediaId
                )
                    .with(user(owner.getEmail())
                        .roles("USER"))
            )
            .andExpect(status().isConflict());

        assertEquals(
            MediaStatus.PENDING_UPLOAD,
            findMedia(mediaId).getStatus()
        );
    }

    @Test
    void requestUpload_OversizedAvatar_ReturnsBadRequest()
        throws Exception {

        long twoMibPlusOne =
            (2L * 1024 * 1024) + 1;

        mockMvc.perform(
                post("/v1/media/uploads")
                    .with(user(owner.getEmail())
                        .roles("USER"))
                    .contentType(
                        MediaType.APPLICATION_JSON
                    )
                    .content("""
                                    {
                                      "purpose": "PROFILE_AVATAR",
                                      "fileName": "avatar.webp",
                                      "contentType": "image/webp",
                                      "sizeBytes": %d
                                    }
                                    """.formatted(
                        twoMibPlusOne
                    ))
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void requestUpload_UnsupportedContentType_ReturnsBadRequest()
        throws Exception {

        mockMvc.perform(
                post("/v1/media/uploads")
                    .with(user(owner.getEmail())
                        .roles("USER"))
                    .contentType(
                        MediaType.APPLICATION_JSON
                    )
                    .content("""
                                        {
                                          "purpose": "EVENT_IMAGE",
                                          "fileName": "event.gif",
                                          "contentType": "image/gif",
                                          "sizeBytes": 1024
                                        }
                                        """)
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void requestUpload_WithoutAuthentication_ReturnsUnauthorized()
        throws Exception {

        mockMvc.perform(
                post("/v1/media/uploads")
                    .contentType(
                        MediaType.APPLICATION_JSON
                    )
                    .content("""
                                        {
                                          "purpose": "EVENT_IMAGE",
                                          "fileName": "event.webp",
                                          "contentType": "image/webp",
                                          "sizeBytes": 1024
                                        }
                                        """)
            )
            .andExpect(status().isUnauthorized());
    }



    private UUID requestUpload(
        User actor,
        String purpose,
        String fileName,
        String contentType,
        long sizeBytes
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                post("/v1/media/uploads")
                    .with(user(actor.getEmail())
                        .roles("USER"))
                    .contentType(
                        MediaType.APPLICATION_JSON
                    )
                    .content("""
                                    {
                                      "purpose": "%s",
                                      "fileName": "%s",
                                      "contentType": "%s",
                                      "sizeBytes": %d
                                    }
                                    """.formatted(
                        purpose,
                        fileName,
                        contentType,
                        sizeBytes
                    ))
            )
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode response = jsonMapper.readTree(
            result.getResponse().getContentAsString()
        );

        return UUID.fromString(
            response.get("mediaId").stringValue()
        );
    }

    private MediaAsset findMedia(UUID mediaId) {
        return mediaRepository.findByPublicId(mediaId)
            .orElseThrow();
    }

    private User createOtherUser() {
        return userRepository.saveAndFlush(
            new User(
                "other-media-user@example.test",
                "Other Media User",
                null
            )
        );
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        FakeObjectStorage fakeObjectStorage() {
            return new FakeObjectStorage();
        }
    }
}
