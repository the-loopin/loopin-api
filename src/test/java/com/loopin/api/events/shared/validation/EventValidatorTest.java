package com.loopin.api.events.shared.validation;

import com.loopin.api.common.exception.DuplicateResourceException;
import com.loopin.api.events.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventValidatorTest {

    private final EventRepository eventRepository = mock(EventRepository.class);
    private final EventValidator validator = new EventValidator(eventRepository);

    @Test
    void validateDateRange_EndBeforeStart_ThrowsBadRequest() {
        LocalDateTime start = LocalDateTime.of(2030, 1, 1, 12, 0);

        assertThrows(IllegalArgumentException.class,
                () -> validator.validateDateRange(start, start.minusMinutes(1)));
    }

    @Test
    void validatePrice_RejectsInvalidFreeAndPaidCombinations() {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validatePrice(true, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class,
                () -> validator.validatePrice(false, BigDecimal.ZERO));
        assertDoesNotThrow(() -> validator.validatePrice(true, BigDecimal.ZERO));
        assertDoesNotThrow(() -> validator.validatePrice(false, BigDecimal.ONE));
    }

    @Test
    void validateNoDuplicate_ExistingEvent_ThrowsConflict() {
        when(eventRepository.exists(any(Specification.class))).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> validator.validateNoDuplicate("Event", "Baku", LocalDateTime.of(2030, 1, 1, 10, 0)));
    }
}
