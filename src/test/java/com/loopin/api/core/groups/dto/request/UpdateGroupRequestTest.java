package com.loopin.api.core.groups.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateGroupRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void maxMembers_BelowMin_ViolatesMinConstraint() {
        UpdateGroupRequest request = new UpdateGroupRequest();
        request.setMaxMembers(0);

        Set<ConstraintViolation<UpdateGroupRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("maxMembers")));
    }

    @Test
    void validRequest_NoViolations() {
        UpdateGroupRequest request = new UpdateGroupRequest();
        request.setMaxMembers(1);

        Set<ConstraintViolation<UpdateGroupRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }
}
