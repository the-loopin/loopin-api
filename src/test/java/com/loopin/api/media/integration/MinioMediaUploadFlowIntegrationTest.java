package com.loopin.api.media.integration;

import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.enums.MediaStatus;
import com.loopin.api.media.repository.MediaAssetRepository;
import com.loopin.api.media.storage.ObjectStorage;
import com.loopin.api.support.AbstractMinioIntegrationTest;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MinioMediaUploadFlowIntegrationTest extends AbstractMinioIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private MediaAssetRepository mediaAssetRepository;
    @Autowired private ObjectStorage objectStorage;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private User owner;

    @BeforeEach
    void setUp() {
        owner = userRepository.saveAndFlush(new User(
                "minio-media-owner@example.test", "MinIO Media Owner", null
        ));
    }

    @Test
    void presignedUploadCompletionMetadataValidationAndDeletionUseMinio() throws Exception {
        MvcResult requestResult = mockMvc.perform(post("/v1/media/uploads")
                        .with(user(owner.getEmail()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "purpose": "EVENT_IMAGE",
                                  "fileName": "event.webp",
                                  "contentType": "image/webp",
                                  "sizeBytes": 18
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = jsonMapper.readTree(requestResult.getResponse().getContentAsString());
        UUID mediaId = UUID.fromString(response.get("mediaId").stringValue());
        URI uploadUrl = URI.create(response.get("uploadUrl").stringValue());
        MediaAsset pending = mediaAssetRepository.findByPublicId(mediaId).orElseThrow();
        byte[] content = "minio-webp-content".getBytes(StandardCharsets.UTF_8);

        HttpResponse<Void> putResponse = httpClient.send(
                HttpRequest.newBuilder(uploadUrl)
                        .header("Content-Type", "image/webp")
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(content))
                        .build(),
                HttpResponse.BodyHandlers.discarding()
        );
        assertThat(putResponse.statusCode()).isBetween(200, 299);
        assertThat(objectStorage.exists(pending.getObjectKey())).isTrue();

        mockMvc.perform(post("/v1/media/uploads/{mediaId}/complete", mediaId)
                        .with(user(owner.getEmail()).roles("USER")))
                .andExpect(status().isOk());

        MediaAsset uploaded = mediaAssetRepository.findByPublicId(mediaId).orElseThrow();
        assertThat(uploaded.getStatus()).isEqualTo(MediaStatus.UPLOADED);
        assertThat(uploaded.getVerifiedFileSize()).isEqualTo((long) content.length);
        assertThat(uploaded.getVerifiedContentType()).isEqualTo("image/webp");

        mockMvc.perform(delete("/v1/media/{mediaId}", mediaId)
                        .with(user(owner.getEmail()).roles("USER")))
                .andExpect(status().isNoContent());

        assertThat(mediaAssetRepository.findByPublicId(mediaId).orElseThrow().getStatus())
                .isEqualTo(MediaStatus.DELETED);
        assertThat(objectStorage.exists(pending.getObjectKey())).isFalse();
    }

    @Test
    void completionWithMismatchedMinioMetadataIsRejectedWithoutPersistingVerifiedFields() throws Exception {
        MvcResult requestResult = mockMvc.perform(post("/v1/media/uploads")
                        .with(user(owner.getEmail()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "purpose": "EVENT_IMAGE",
                                  "fileName": "declared.webp",
                                  "contentType": "image/webp",
                                  "sizeBytes": 18
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = jsonMapper.readTree(requestResult.getResponse().getContentAsString());
        UUID mediaId = UUID.fromString(response.get("mediaId").stringValue());
        MediaAsset pending = mediaAssetRepository.findByPublicId(mediaId).orElseThrow();
        byte[] wrongSizeContent = "too-short".getBytes(StandardCharsets.UTF_8);

        HttpResponse<Void> putResponse = httpClient.send(
                HttpRequest.newBuilder(URI.create(response.get("uploadUrl").stringValue()))
                        .header("Content-Type", "image/webp")
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(wrongSizeContent))
                        .build(),
                HttpResponse.BodyHandlers.discarding()
        );
        assertThat(putResponse.statusCode()).isBetween(200, 299);

        mockMvc.perform(post("/v1/media/uploads/{mediaId}/complete", mediaId)
                        .with(user(owner.getEmail()).roles("USER")))
                .andExpect(status().isConflict());

        MediaAsset rejected = mediaAssetRepository.findByPublicId(mediaId).orElseThrow();
        assertThat(rejected.getStatus()).isEqualTo(MediaStatus.PENDING_UPLOAD);
        assertThat(rejected.getVerifiedFileSize()).isNull();
        assertThat(rejected.getVerifiedContentType()).isNull();
        assertThat(objectStorage.exists(pending.getObjectKey())).isFalse();
    }
}
