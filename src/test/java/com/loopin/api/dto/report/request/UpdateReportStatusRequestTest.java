package com.loopin.api.dto.report.request;

import com.loopin.api.common.enums.ReportStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateReportStatusRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void status_Null_ViolatesNotNullConstraint() {
        UpdateReportStatusRequest request = new UpdateReportStatusRequest();
        request.setStatus(null);

        Set<ConstraintViolation<UpdateReportStatusRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("status")));
    }

    @Test
    void validRequest_NoViolations() {
        UpdateReportStatusRequest request = new UpdateReportStatusRequest();
        request.setStatus(ReportStatus.RESOLVED);

        Set<ConstraintViolation<UpdateReportStatusRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }
}
