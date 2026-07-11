package com.loopin.api.media.requestupload;

import com.loopin.api.media.dto.request.RequestMediaUploadRequest;
import com.loopin.api.media.dto.response.MediaUploadResponse;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.repository.MediaAssetRepository;
import com.loopin.api.media.shared.key.MediaObjectKeyGenerator;
import com.loopin.api.media.shared.policy.MediaUploadPolicy;
import com.loopin.api.media.storage.ObjectStorage;
import com.loopin.api.media.storage.PresignedUpload;
import com.loopin.api.media.storage.StorageProperties;
import com.loopin.api.users.api.UserLookup;
import com.loopin.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RequestMediaUploadHandler {

    private final UserLookup userLookup;
    private final MediaAssetRepository mediaAssetRepository;
    private final MediaUploadPolicy mediaUploadPolicy;
    private final MediaObjectKeyGenerator objectKeyGenerator;
    private final ObjectStorage objectStorage;
    private final StorageProperties storageProperties;

    @Transactional
    public MediaUploadResponse handle(RequestMediaUploadCommand command) {
        RequestMediaUploadRequest request = command.request();

        User owner = userLookup.findByEmail(command.currentUserEmail());

        String normalizedContentType = request.getContentType()
            .trim()
            .toLowerCase(Locale.ROOT);

        String normalizedFileName = request.getFileName().trim();

        mediaUploadPolicy.validateRequest(
            request.getPurpose(),
            normalizedContentType,
            request.getSizeBytes()
        );

        UUID mediaPublicId = UUID.randomUUID();

        String objectKey = objectKeyGenerator.generate(
            owner.getPublicId(),
            request.getPurpose(),
            mediaPublicId
        );

        MediaAsset media = MediaAsset.pending(
            mediaPublicId,
            owner,
            objectKey,
            normalizedFileName,
            normalizedContentType,
            request.getSizeBytes(),
            request.getPurpose()
        );

        mediaAssetRepository.save(media);

        PresignedUpload upload = objectStorage.createUploadUrl(
            objectKey,
            normalizedContentType,
            storageProperties.getUploadUrlTtl()
        );

        return new MediaUploadResponse(
            mediaPublicId,
            upload.uploadUrl().toString(),
            upload.expiresAt(),
            Map.of("Content-Type", normalizedContentType)
        );
    }
}
