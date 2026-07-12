package com.loopin.api.events.shared.validation;

import com.loopin.api.events.dto.request.EventCreateRequest;
import com.loopin.api.events.dto.request.EventRequest;
import com.loopin.api.events.dto.request.EventUpdateRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventRequestValidatorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2030, 1, 1, 10, 0);
    private final EventRequestValidator validator = new EventRequestValidator(
            Clock.fixed(Instant.parse("2030-01-01T10:00:00Z"), ZoneOffset.UTC));

    @Test
    void rejectsPastAndInsufficientLeadTimesUsingInjectedClock() {
        EventCreateRequest past = validCreateRequest();
        past.setStartDateTime(NOW.minusMinutes(1));
        past.setEndDateTime(NOW.plusHours(1));
        assertField(past, "startDateTime");

        EventCreateRequest insufficientLeadTime = validCreateRequest();
        insufficientLeadTime.setStartDateTime(NOW.plusMinutes(29));
        insufficientLeadTime.setEndDateTime(NOW.plusHours(1));
        assertField(insufficientLeadTime, "startDateTime");
    }

    @Test
    void rejectsEqualEarlierAndOverlongEndTimes() {
        EventCreateRequest request = validCreateRequest();
        request.setEndDateTime(request.getStartDateTime());
        assertField(request, "endDateTime");

        request = validCreateRequest();
        request.setEndDateTime(request.getStartDateTime().minusMinutes(1));
        assertField(request, "endDateTime");

        request = validCreateRequest();
        request.setEndDateTime(request.getStartDateTime().plusHours(24).plusMinutes(1));
        assertField(request, "endDateTime");
    }

    @Test
    void enforcesFreeAndPaidPriceRules() {
        EventCreateRequest free = validCreateRequest();
        free.setPrice(BigDecimal.ONE);
        assertField(free, "price");

        for (BigDecimal price : List.of(BigDecimal.ZERO, new BigDecimal("-1.00"), new BigDecimal("10.999"), new BigDecimal("1000000.01"))) {
            EventCreateRequest paid = validCreateRequest();
            paid.setIsFree(false);
            paid.setPrice(price);
            assertField(paid, "price");
        }

        EventCreateRequest missingPaidPrice = validCreateRequest();
        missingPaidPrice.setIsFree(false);
        missingPaidPrice.setPrice(null);
        assertField(missingPaidPrice, "price");
    }

    @Test
    void rejectsInvalidUrlCoordinatesAndInterestIds() {
        EventCreateRequest invalidUrl = validCreateRequest();
        invalidUrl.setImageUrl("ftp://example.com/image.jpg");
        assertField(invalidUrl, "imageUrl");

        EventCreateRequest latitudeOnly = validCreateRequest();
        latitudeOnly.setLatitude(BigDecimal.ONE);
        assertField(latitudeOnly, "coordinates");

        EventCreateRequest longitudeOnly = validCreateRequest();
        longitudeOnly.setLongitude(BigDecimal.ONE);
        assertField(longitudeOnly, "coordinates");

        EventCreateRequest tooMany = validCreateRequest();
        tooMany.setInterestIds(java.util.stream.IntStream.range(0, 11).mapToObj(i -> UUID.randomUUID()).toList());
        assertField(tooMany, "interestIds");

        EventCreateRequest nullInterest = validCreateRequest();
        nullInterest.setInterestIds(java.util.Arrays.asList(UUID.randomUUID(), null));
        assertField(nullInterest, "interestIds");

        EventCreateRequest duplicateInterest = validCreateRequest();
        UUID id = UUID.randomUUID();
        duplicateInterest.setInterestIds(List.of(id, id));
        assertField(duplicateInterest, "interestIds");
    }

    @Test
    void acceptsEquivalentValidCreateAndUpdateRequests() {
        assertDoesNotThrow(() -> validator.validate(validCreateRequest()));

        EventUpdateRequest update = new EventUpdateRequest();
        update.setStartDateTime(NOW.plusHours(1));
        update.setEndDateTime(NOW.plusHours(2));
        update.setIsFree(false);
        update.setPrice(new BigDecimal("12.50"));
        update.setLatitude(new BigDecimal("40.4093"));
        update.setLongitude(new BigDecimal("49.8671"));
        update.setImageUrl("https://example.com/image.jpg");
        update.setInterestIds(List.of(UUID.randomUUID()));
        assertDoesNotThrow(() -> validator.validate(update));
    }

    @Test
    void acceptsExactTimeBoundaries() {
        EventCreateRequest minimumLeadTime = validCreateRequest();
        minimumLeadTime.setStartDateTime(NOW.plusMinutes(30));
        minimumLeadTime.setEndDateTime(NOW.plusHours(2));
        assertDoesNotThrow(() -> validator.validate(minimumLeadTime));

        EventCreateRequest maximumDuration = validCreateRequest();
        maximumDuration.setEndDateTime(maximumDuration.getStartDateTime().plusHours(24));
        assertDoesNotThrow(() -> validator.validate(maximumDuration));
    }

    @Test
    void acceptsExactPriceAndInterestBoundaries() {
        for (BigDecimal price : List.of(new BigDecimal("0.01"), new BigDecimal("1000000.00"))) {
            EventCreateRequest request = validCreateRequest();
            request.setIsFree(false);
            request.setPrice(price);
            assertDoesNotThrow(() -> validator.validate(request));
        }

        EventCreateRequest tenInterests = validCreateRequest();
        tenInterests.setInterestIds(java.util.stream.IntStream.range(0, 10)
                .mapToObj(index -> UUID.randomUUID())
                .toList());
        assertDoesNotThrow(() -> validator.validate(tenInterests));
    }

    private EventCreateRequest validCreateRequest() {
        EventCreateRequest request = new EventCreateRequest();
        request.setStartDateTime(NOW.plusHours(1));
        request.setEndDateTime(NOW.plusHours(2));
        request.setIsFree(true);
        request.setPrice(BigDecimal.ZERO);
        request.setInterestIds(List.of());
        return request;
    }

    private void assertField(EventRequest request, String field) {
        EventRequestValidationException exception = assertThrows(
                EventRequestValidationException.class,
                () -> validator.validate(request));
        assertTrue(exception.getFieldErrors().containsKey(field));
    }
}
