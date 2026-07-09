package com.loopin.api.dto.event.request;

import com.loopin.api.common.enums.EventCategory;
import com.loopin.api.common.enums.EventType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventCreateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private EventCreateRequest createValidRequest() {
        EventCreateRequest request = new EventCreateRequest();
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
        EventCreateRequest request = createValidRequest();
        request.setTitle("");

        Set<ConstraintViolation<EventCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }

    @Test
    void title_ExceedsMaxLength_ViolatesSizeConstraint() {
        EventCreateRequest request = createValidRequest();
        request.setTitle("a".repeat(121));

        Set<ConstraintViolation<EventCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }

    @Test
    void title_ExactMaxLength_IsValid() {
        EventCreateRequest request = createValidRequest();
        request.setTitle("a".repeat(120));

        Set<ConstraintViolation<EventCreateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void description_Blank_ViolatesNotBlankConstraint() {
        EventCreateRequest request = createValidRequest();
        request.setDescription("");

        Set<ConstraintViolation<EventCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("description")));
    }

    @Test
    void description_ExceedsMaxLength_ViolatesSizeConstraint() {
        EventCreateRequest request = createValidRequest();
        request.setDescription("a".repeat(2001));

        Set<ConstraintViolation<EventCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("description")));
    }

    @Test
    void description_ExactMaxLength_IsValid() {
        EventCreateRequest request = createValidRequest();
        request.setDescription("a".repeat(2000));

        Set<ConstraintViolation<EventCreateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void type_Null_ViolatesNotNullConstraint() {
        EventCreateRequest request = createValidRequest();
        request.setType(null);

        Set<ConstraintViolation<EventCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("type")));
    }

    @Test
    void category_Null_ViolatesNotNullConstraint() {
        EventCreateRequest request = createValidRequest();
        request.setCategory(null);

        Set<ConstraintViolation<EventCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("category")));
    }

    @Test
    void city_Blank_ViolatesNotBlankConstraint() {
        EventCreateRequest request = createValidRequest();
        request.setCity("");

        Set<ConstraintViolation<EventCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("city")));
    }

    @Test
    void city_ExceedsMaxLength_ViolatesSizeConstraint() {
        EventCreateRequest request = createValidRequest();
        request.setCity("a".repeat(101));

        Set<ConstraintViolation<EventCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("city")));
    }

    @Test
    void city_ExactMaxLength_IsValid() {
        EventCreateRequest request = createValidRequest();
        request.setCity("a".repeat(100));

        Set<ConstraintViolation<EventCreateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void address_ExceedsMaxLength_ViolatesSizeConstraint() {
        EventCreateRequest request = createValidRequest();
        request.setAddress("a".repeat(256));

        Set<ConstraintViolation<EventCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("address")));
    }

    @Test
    void address_ExactMaxLength_IsValid() {
        EventCreateRequest request = createValidRequest();
        request.setAddress("a".repeat(255));

        Set<ConstraintViolation<EventCreateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void coordinates_ValidBoundaryValues_AreValid() {
        EventCreateRequest request = createValidRequest();
        request.setLatitude(new BigDecimal("90.0"));
        request.setLongitude(new BigDecimal("-180.0"));

        Set<ConstraintViolation<EventCreateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void latitude_OutOfRange_ViolatesDecimalConstraints() {
        EventCreateRequest request = createValidRequest();
        request.setLatitude(new BigDecimal("90.1"));

        Set<ConstraintViolation<EventCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("latitude")));
    }

    @Test
    void longitude_OutOfRange_ViolatesDecimalConstraints() {
        EventCreateRequest request = createValidRequest();
        request.setLongitude(new BigDecimal("-180.1"));

        Set<ConstraintViolation<EventCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("longitude")));
    }

    @Test
    void startDateTime_Null_ViolatesNotNullConstraint() {
        EventCreateRequest request = createValidRequest();
        request.setStartDateTime(null);

        Set<ConstraintViolation<EventCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("startDateTime")));
    }

    @Test
    void endDateTime_Null_ViolatesNotNullConstraint() {
        EventCreateRequest request = createValidRequest();
        request.setEndDateTime(null);

        Set<ConstraintViolation<EventCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("endDateTime")));
    }

    @Test
    void isFree_Null_ViolatesNotNullConstraint() {
        EventCreateRequest request = createValidRequest();
        request.setIsFree(null);

        Set<ConstraintViolation<EventCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("isFree")));
    }

    @Test
    void organizerName_Blank_ViolatesNotBlankConstraint() {
        EventCreateRequest request = createValidRequest();
        request.setOrganizerName("");

        Set<ConstraintViolation<EventCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("organizerName")));
    }

    @Test
    void organizerName_ExceedsMaxLength_ViolatesSizeConstraint() {
        EventCreateRequest request = createValidRequest();
        request.setOrganizerName("a".repeat(121));

        Set<ConstraintViolation<EventCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("organizerName")));
    }

    @Test
    void organizerName_ExactMaxLength_IsValid() {
        EventCreateRequest request = createValidRequest();
        request.setOrganizerName("a".repeat(120));

        Set<ConstraintViolation<EventCreateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void imageUrl_ExceedsMaxLength_ViolatesSizeConstraint() {
        EventCreateRequest request = createValidRequest();
        request.setImageUrl("a".repeat(501));

        Set<ConstraintViolation<EventCreateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("imageUrl")));
    }

    @Test
    void imageUrl_ExactMaxLength_IsValid() {
        EventCreateRequest request = createValidRequest();
        request.setImageUrl("a".repeat(500));

        Set<ConstraintViolation<EventCreateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validRequest_NoViolations() {
        EventCreateRequest request = createValidRequest();

        Set<ConstraintViolation<EventCreateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }
}
