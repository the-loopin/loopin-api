package com.loopin.api.dto.report.request;

import com.loopin.api.common.enums.ReportTargetType;
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

class CreateReportRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private CreateReportRequest createValidRequest() {
        CreateReportRequest request = new CreateReportRequest();
        request.setTargetType(ReportTargetType.GROUP);
        request.setTargetId(UUID.randomUUID());
        request.setReason("Valid reason");
        request.setDetails("Valid details");
        return request;
    }

    @Test
    void targetType_Null_ViolatesNotNullConstraint() {
        CreateReportRequest request = createValidRequest();
        request.setTargetType(null);

        Set<ConstraintViolation<CreateReportRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("targetType")));
    }

    @Test
    void targetId_Null_ViolatesNotNullConstraint() {
        CreateReportRequest request = createValidRequest();
        request.setTargetId(null);

        Set<ConstraintViolation<CreateReportRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("targetId")));
    }

    @Test
    void reason_Blank_ViolatesNotBlankConstraint() {
        CreateReportRequest request = createValidRequest();
        request.setReason("   ");

        Set<ConstraintViolation<CreateReportRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("reason")));
    }

    @Test
    void reason_ExceedsMaxLength_ViolatesSizeConstraint() {
        CreateReportRequest request = createValidRequest();
        request.setReason("a".repeat(501));

        Set<ConstraintViolation<CreateReportRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("reason")));
    }

    @Test
    void reason_ExactMaxLength_IsValid() {
        CreateReportRequest request = createValidRequest();
        request.setReason("a".repeat(500));

        Set<ConstraintViolation<CreateReportRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void details_ExceedsMaxLength_ViolatesSizeConstraint() {
        CreateReportRequest request = createValidRequest();
        request.setDetails("a".repeat(2001));

        Set<ConstraintViolation<CreateReportRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("details")));
    }

    @Test
    void details_ExactMaxLength_IsValid() {
        CreateReportRequest request = createValidRequest();
        request.setDetails("a".repeat(2000));

        Set<ConstraintViolation<CreateReportRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validRequest_NoViolations() {
        CreateReportRequest request = createValidRequest();

        Set<ConstraintViolation<CreateReportRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }
}
