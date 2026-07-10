package com.loopin.api.groups.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateGroupJoinRequestRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void message_ExceedsMaxLength_ViolatesSizeConstraint() {
        CreateGroupJoinRequestRequest request = new CreateGroupJoinRequestRequest();
        request.setMessage("a".repeat(501));

        Set<ConstraintViolation<CreateGroupJoinRequestRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("message")));
    }

    @Test
    void validRequest_NoViolations() {
        CreateGroupJoinRequestRequest request = new CreateGroupJoinRequestRequest();
        request.setMessage("a".repeat(500));

        Set<ConstraintViolation<CreateGroupJoinRequestRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }
}
