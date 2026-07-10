package com.loopin.api.events.shared.interest;

import com.loopin.api.events.entity.Event;
import com.loopin.api.interests.repository.InterestRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventInterestManagerTest {

    private final InterestRepository interestRepository = mock(InterestRepository.class);
    private final EventInterestManager manager = new EventInterestManager(interestRepository);

    @Test
    void replace_DuplicateInterestIdentifiers_ThrowsBadRequestBeforeLookup() {
        UUID interestId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class,
                () -> manager.replace(new Event(), List.of(interestId, interestId)));

        verify(interestRepository, never()).findByPublicIdInAndDeletedAtIsNull(any());
    }

    @Test
    void replace_UnknownInterestIdentifier_ThrowsNotFound() {
        when(interestRepository.findByPublicIdInAndDeletedAtIsNull(any())).thenReturn(List.of());

        assertThrows(NoSuchElementException.class,
                () -> manager.replace(new Event(), List.of(UUID.randomUUID())));
    }
}
