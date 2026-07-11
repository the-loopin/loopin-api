package com.loopin.api.media.shared.finder;

import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.repository.MediaAssetRepository;
import com.loopin.api.media.support.MediaTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MediaFinderTest {

    private MediaAssetRepository repository;
    private MediaFinder finder;

    @BeforeEach
    void setUp() {
        repository = mock(MediaAssetRepository.class);
        finder = new MediaFinder(repository);
    }

    @Test
    void findByPublicId_ExistingMedia_ReturnsMedia() {
        MediaAsset media =
            MediaTestFixtures.pendingMedia();

        when(repository.findByPublicId(
            media.getPublicId()
        )).thenReturn(Optional.of(media));

        MediaAsset result =
            finder.findByPublicId(media.getPublicId());

        assertEquals(media, result);
    }

    @Test
    void findByPublicId_MissingMedia_ThrowsNotFound() {
        UUID mediaId = UUID.randomUUID();

        when(repository.findByPublicId(mediaId))
            .thenReturn(Optional.empty());

        assertThrows(
            ResourceNotFoundException.class,
            () -> finder.findByPublicId(mediaId)
        );
    }
}
