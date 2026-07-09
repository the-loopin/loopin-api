package com.loopin.api.chat.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateGroupMessageRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void messageText_Blank_ViolatesNotBlankConstraint() {
        CreateGroupMessageRequest request = new CreateGroupMessageRequest();
        request.setMessageText("   ");

        Set<ConstraintViolation<CreateGroupMessageRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("messageText")));
    }

    @Test
    void messageText_ExceedsMaxLength_ViolatesSizeConstraint() {
        CreateGroupMessageRequest request = new CreateGroupMessageRequest();
        request.setMessageText("a".repeat(1001));

        Set<ConstraintViolation<CreateGroupMessageRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("messageText")));
    }

    @Test
    void messageText_ExactMaxLength_IsValid() {
        CreateGroupMessageRequest request = new CreateGroupMessageRequest();
        request.setMessageText("a".repeat(1000));

        Set<ConstraintViolation<CreateGroupMessageRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validRequest_NoViolations() {
        CreateGroupMessageRequest request = new CreateGroupMessageRequest();
        request.setMessageText("Valid message");

        Set<ConstraintViolation<CreateGroupMessageRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }
}
