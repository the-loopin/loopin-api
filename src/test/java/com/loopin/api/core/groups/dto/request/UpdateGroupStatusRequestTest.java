package com.loopin.api.core.groups.dto.request;

import com.loopin.api.core.groups.enums.GroupStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateGroupStatusRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void status_Null_ViolatesNotNullConstraint() {
        UpdateGroupStatusRequest request = new UpdateGroupStatusRequest();
        request.setStatus(null);

        Set<ConstraintViolation<UpdateGroupStatusRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("status")));
    }

    @Test
    void validRequest_NoViolations() {
        UpdateGroupStatusRequest request = new UpdateGroupStatusRequest();
        request.setStatus(GroupStatus.OPEN);

        Set<ConstraintViolation<UpdateGroupStatusRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }
}
