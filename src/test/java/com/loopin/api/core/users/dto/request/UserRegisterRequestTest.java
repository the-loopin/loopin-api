package com.loopin.api.core.users.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRegisterRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private UserRegisterRequest createValidRequest() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setEmail("test@example.com");
        request.setName("Test User");
        return request;
    }

    @Test
    void email_Blank_ViolatesNotBlankConstraint() {
        UserRegisterRequest request = createValidRequest();
        request.setEmail("   ");

        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    void email_Malformed_ViolatesEmailConstraint() {
        UserRegisterRequest request = createValidRequest();
        request.setEmail("invalid-email-format");

        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    void name_Blank_ViolatesNotBlankConstraint() {
        UserRegisterRequest request = createValidRequest();
        request.setName("   ");

        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    void validRequest_NoViolations() {
        UserRegisterRequest request = createValidRequest();

        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }
}
