package com.loopin.api.events.shared.validation;

import com.loopin.api.common.exception.DuplicateResourceException;
import com.loopin.api.events.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventValidatorTest {

    private final EventRepository eventRepository = mock(EventRepository.class);
    private final EventValidator validator = new EventValidator(eventRepository);

    @Test
    void validateNoDuplicate_ExistingEvent_ThrowsConflict() {
        when(eventRepository.exists(any(Specification.class))).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> validator.validateNoDuplicate("Event", "Baku", LocalDateTime.of(2030, 1, 1, 10, 0)));
    }
}
