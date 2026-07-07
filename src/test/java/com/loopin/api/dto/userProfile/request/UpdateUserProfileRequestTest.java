package com.loopin.api.dto.userProfile.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateUserProfileRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private UpdateUserProfileRequest createValidRequest() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setName("Valid Name");
        request.setCity("Valid City");
        request.setBio("Valid Bio");
        return request;
    }

    @Test
    void name_Blank_ViolatesNotBlankConstraint() {
        UpdateUserProfileRequest request = createValidRequest();
        request.setName("   ");

        Set<ConstraintViolation<UpdateUserProfileRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    void name_ExceedsMaxLength_ViolatesSizeConstraint() {
        UpdateUserProfileRequest request = createValidRequest();
        request.setName("a".repeat(101));

        Set<ConstraintViolation<UpdateUserProfileRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    void name_ExactMaxLength_IsValid() {
        UpdateUserProfileRequest request = createValidRequest();
        request.setName("a".repeat(100));

        Set<ConstraintViolation<UpdateUserProfileRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void city_Blank_ViolatesNotBlankConstraint() {
        UpdateUserProfileRequest request = createValidRequest();
        request.setCity("   ");

        Set<ConstraintViolation<UpdateUserProfileRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("city")));
    }

    @Test
    void city_ExceedsMaxLength_ViolatesSizeConstraint() {
        UpdateUserProfileRequest request = createValidRequest();
        request.setCity("a".repeat(51));

        Set<ConstraintViolation<UpdateUserProfileRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("city")));
    }

    @Test
    void city_ExactMaxLength_IsValid() {
        UpdateUserProfileRequest request = createValidRequest();
        request.setCity("a".repeat(50));

        Set<ConstraintViolation<UpdateUserProfileRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void bio_ExceedsMaxLength_ViolatesSizeConstraint() {
        UpdateUserProfileRequest request = createValidRequest();
        request.setBio("a".repeat(501));

        Set<ConstraintViolation<UpdateUserProfileRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("bio")));
    }

    @Test
    void bio_ExactMaxLength_IsValid() {
        UpdateUserProfileRequest request = createValidRequest();
        request.setBio("a".repeat(500));

        Set<ConstraintViolation<UpdateUserProfileRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validRequest_NoViolations() {
        UpdateUserProfileRequest request = createValidRequest();

        Set<ConstraintViolation<UpdateUserProfileRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }
}
