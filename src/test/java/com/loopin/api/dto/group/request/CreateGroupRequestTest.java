package com.loopin.api.dto.group.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateGroupRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private CreateGroupRequest createValidRequest() {
        CreateGroupRequest request = new CreateGroupRequest();
        request.setEventId(UUID.randomUUID());
        request.setTitle("Valid Title");
        request.setMaxMembers(1);
        return request;
    }

    @Test
    void eventId_Null_ViolatesNotNullConstraint() {
        CreateGroupRequest request = createValidRequest();
        request.setEventId(null);

        Set<ConstraintViolation<CreateGroupRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("eventId")));
    }

    @Test
    void title_Blank_ViolatesNotBlankConstraint() {
        CreateGroupRequest request = createValidRequest();
        request.setTitle("   ");

        Set<ConstraintViolation<CreateGroupRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }

    @Test
    void maxMembers_BelowMin_ViolatesMinConstraint() {
        CreateGroupRequest request = createValidRequest();
        request.setMaxMembers(0);

        Set<ConstraintViolation<CreateGroupRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("maxMembers")));
    }

    @Test
    void validRequest_NoViolations() {
        CreateGroupRequest request = createValidRequest();

        Set<ConstraintViolation<CreateGroupRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }
}
