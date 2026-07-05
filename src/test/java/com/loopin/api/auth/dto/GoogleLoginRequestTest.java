package com.loopin.api.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleLoginRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void idToken_Blank_ViolatesNotBlankConstraint() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("");

        Set<ConstraintViolation<GoogleLoginRequest>> violations = validator.validate(request);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("idToken")));
    }

    @Test
    void validRequest_NoViolations() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("valid.jwt.token");

        Set<ConstraintViolation<GoogleLoginRequest>> violations = validator.validate(request);
        
        assertTrue(violations.isEmpty());
    }
}
