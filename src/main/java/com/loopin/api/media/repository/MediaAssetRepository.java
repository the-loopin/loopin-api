package com.loopin.api.media.repository;

import com.loopin.api.media.entity.MediaAsset;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {

    Optional<MediaAsset> findByPublicId(UUID publicId);

    boolean existsByObjectKey(String objectKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select media
        from MediaAsset media
        where media.publicId = :publicId
    """)
    Optional<MediaAsset> findByPublicIdForUpdate(UUID publicId);
}
