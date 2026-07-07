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

class GroupMemberRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void userId_Null_ViolatesNotNullConstraint() {
        GroupMemberRequest request = new GroupMemberRequest();
        request.setUserId(null);

        Set<ConstraintViolation<GroupMemberRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("userId")));
    }

    @Test
    void validRequest_NoViolations() {
        GroupMemberRequest request = new GroupMemberRequest();
        request.setUserId(UUID.randomUUID());

        Set<ConstraintViolation<GroupMemberRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }
}
