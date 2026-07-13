package com.loopin.api.media.entity;

import com.loopin.api.common.entity.BaseEntity;
import com.loopin.api.common.exception.InvalidMediaStateException;
import com.loopin.api.media.enums.MediaPurpose;
import com.loopin.api.media.enums.MediaStatus;
import com.loopin.api.media.storage.StoredObjectMetadata;
import com.loopin.api.users.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "media_assets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MediaAsset extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "object_key", nullable = false, unique = true, length = 512)
    private String objectKey;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "declared_content_type", nullable = false, length = 100)
    private String declaredContentType;

    @Column(name = "verified_content_type", length = 100)
    private String verifiedContentType;

    @Column(name = "declared_file_size", nullable = false)
    private Long declaredFileSize;

    @Column(name = "verified_file_size")
    private Long verifiedFileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MediaPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MediaStatus status;

    public static MediaAsset pending(
        UUID publicId,
        User owner,
        String objectKey,
        String originalFilename,
        String contentType,
        long sizeBytes,
        MediaPurpose purpose
    ) {
        MediaAsset asset = new MediaAsset();
        asset.setPublicId(publicId);
        asset.owner = owner;
        asset.objectKey = objectKey;
        asset.originalFilename = originalFilename;
        asset.declaredContentType = contentType;
        asset.declaredFileSize = sizeBytes;
        asset.purpose = purpose;
        asset.status = MediaStatus.PENDING_UPLOAD;
        return asset;
    }

    public void markUploaded(
        StoredObjectMetadata metadata
    ) {
        if (status != MediaStatus.PENDING_UPLOAD) {
            throw new InvalidMediaStateException(
                "Only pending media can be completed"
            );
        }

        this.verifiedContentType =
            metadata.contentType();

        this.verifiedFileSize =
            metadata.sizeBytes();

        this.status = MediaStatus.UPLOADED;
    }

    public void markDeleted() {
        if (status == MediaStatus.ATTACHED) {
            throw new InvalidMediaStateException(
                "Attached media cannot be deleted"
            );
        }

        if (status == MediaStatus.DELETED) {
            return;
        }

        this.status = MediaStatus.DELETED;
        markAsDeleted();
    }

    public void markAttached() {
        if (status != MediaStatus.UPLOADED) {
            throw new InvalidMediaStateException(
                "Only uploaded media can be attached"
            );
        }

        this.status = MediaStatus.ATTACHED;
    }

    public void markDetached() {
        if (status != MediaStatus.ATTACHED) {
            throw new InvalidMediaStateException(
                "Only attached media can be detached"
            );
        }

        this.status = MediaStatus.UPLOADED;
    }
}
