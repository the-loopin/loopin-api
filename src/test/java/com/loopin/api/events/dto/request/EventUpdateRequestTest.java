package com.loopin.api.events.dto.request;

import com.loopin.api.events.enums.EventCategory;
import com.loopin.api.events.enums.EventType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventUpdateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private EventUpdateRequest createValidRequest() {
        EventUpdateRequest request = new EventUpdateRequest();
        request.setTitle("Valid Title");
        request.setDescription("Valid Description");
        request.setType(EventType.EVENT);
        request.setCategory(EventCategory.TECH);
        request.setCity("Valid City");
        request.setStartDateTime(LocalDateTime.now().plusDays(1));
        request.setEndDateTime(LocalDateTime.now().plusDays(2));
        request.setIsFree(true);
        request.setOrganizerName("Valid Organizer");
        return request;
    }

    @Test
    void title_Blank_ViolatesNotBlankConstraint() {
        EventUpdateRequest request = createValidRequest();
        request.setTitle("");

        Set<ConstraintViolation<EventUpdateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }

    @Test
    void title_ExceedsMaxLength_ViolatesSizeConstraint() {
        EventUpdateRequest request = createValidRequest();
        request.setTitle("a".repeat(121));

        Set<ConstraintViolation<EventUpdateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }

    @Test
    void title_ExactMaxLength_IsValid() {
        EventUpdateRequest request = createValidRequest();
        request.setTitle("a".repeat(120));

        Set<ConstraintViolation<EventUpdateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void description_Blank_ViolatesNotBlankConstraint() {
        EventUpdateRequest request = createValidRequest();
        request.setDescription("");

        Set<ConstraintViolation<EventUpdateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("description")));
    }

    @Test
    void description_ExceedsMaxLength_ViolatesSizeConstraint() {
        EventUpdateRequest request = createValidRequest();
        request.setDescription("a".repeat(2001));

        Set<ConstraintViolation<EventUpdateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("description")));
    }

    @Test
    void description_ExactMaxLength_IsValid() {
        EventUpdateRequest request = createValidRequest();
        request.setDescription("a".repeat(2000));

        Set<ConstraintViolation<EventUpdateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void type_Null_ViolatesNotNullConstraint() {
        EventUpdateRequest request = createValidRequest();
        request.setType(null);

        Set<ConstraintViolation<EventUpdateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("type")));
    }

    @Test
    void category_Null_ViolatesNotNullConstraint() {
        EventUpdateRequest request = createValidRequest();
        request.setCategory(null);

        Set<ConstraintViolation<EventUpdateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("category")));
    }

    @Test
    void city_Blank_ViolatesNotBlankConstraint() {
        EventUpdateRequest request = createValidRequest();
        request.setCity("");

        Set<ConstraintViolation<EventUpdateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("city")));
    }

    @Test
    void city_ExceedsMaxLength_ViolatesSizeConstraint() {
        EventUpdateRequest request = createValidRequest();
        request.setCity("a".repeat(101));

        Set<ConstraintViolation<EventUpdateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("city")));
    }

    @Test
    void city_ExactMaxLength_IsValid() {
        EventUpdateRequest request = createValidRequest();
        request.setCity("a".repeat(100));

        Set<ConstraintViolation<EventUpdateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void address_ExceedsMaxLength_ViolatesSizeConstraint() {
        EventUpdateRequest request = createValidRequest();
        request.setAddress("a".repeat(256));

        Set<ConstraintViolation<EventUpdateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("address")));
    }

    @Test
    void address_ExactMaxLength_IsValid() {
        EventUpdateRequest request = createValidRequest();
        request.setAddress("a".repeat(255));

        Set<ConstraintViolation<EventUpdateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void coordinates_ValidBoundaryValues_AreValid() {
        EventUpdateRequest request = createValidRequest();
        request.setLatitude(new BigDecimal("-90.0"));
        request.setLongitude(new BigDecimal("180.0"));

        Set<ConstraintViolation<EventUpdateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void latitude_OutOfRange_ViolatesDecimalConstraints() {
        EventUpdateRequest request = createValidRequest();
        request.setLatitude(new BigDecimal("-90.1"));

        Set<ConstraintViolation<EventUpdateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("latitude")));
    }

    @Test
    void longitude_OutOfRange_ViolatesDecimalConstraints() {
        EventUpdateRequest request = createValidRequest();
        request.setLongitude(new BigDecimal("180.1"));

        Set<ConstraintViolation<EventUpdateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("longitude")));
    }

    @Test
    void startDateTime_Null_ViolatesNotNullConstraint() {
        EventUpdateRequest request = createValidRequest();
        request.setStartDateTime(null);

        Set<ConstraintViolation<EventUpdateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("startDateTime")));
    }

    @Test
    void endDateTime_Null_ViolatesNotNullConstraint() {
        EventUpdateRequest request = createValidRequest();
        request.setEndDateTime(null);

        Set<ConstraintViolation<EventUpdateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("endDateTime")));
    }

    @Test
    void isFree_Null_ViolatesNotNullConstraint() {
        EventUpdateRequest request = createValidRequest();
        request.setIsFree(null);

        Set<ConstraintViolation<EventUpdateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("isFree")));
    }

    @Test
    void organizerName_Blank_ViolatesNotBlankConstraint() {
        EventUpdateRequest request = createValidRequest();
        request.setOrganizerName("");

        Set<ConstraintViolation<EventUpdateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("organizerName")));
    }

    @Test
    void organizerName_ExceedsMaxLength_ViolatesSizeConstraint() {
        EventUpdateRequest request = createValidRequest();
        request.setOrganizerName("a".repeat(121));

        Set<ConstraintViolation<EventUpdateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("organizerName")));
    }

    @Test
    void organizerName_ExactMaxLength_IsValid() {
        EventUpdateRequest request = createValidRequest();
        request.setOrganizerName("a".repeat(120));

        Set<ConstraintViolation<EventUpdateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void imageMediaId_validUuid_isValid() {
        EventUpdateRequest request =
            createValidRequest();

        request.setImageMediaId(
            UUID.randomUUID()
        );

        Set<ConstraintViolation<EventUpdateRequest>>
            violations =
            validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validRequest_NoViolations() {
        EventUpdateRequest request = createValidRequest();

        Set<ConstraintViolation<EventUpdateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }
}
