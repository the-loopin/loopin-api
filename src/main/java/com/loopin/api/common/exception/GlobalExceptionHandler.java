package com.loopin.api.common.exception;

import com.loopin.api.common.exception.dto.ErrorResponse;
import com.loopin.api.events.shared.validation.EventRequestValidationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
        NoSuchElementException.class,
        ResourceNotFoundException.class,
        org.springframework.web.servlet.resource.NoResourceFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(
            Exception exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = buildErrorResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            org.springframework.http.converter.HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid request payload or value",
                request.getRequestURI(),
                null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = buildErrorResponse(
                HttpStatus.FORBIDDEN,
                exception.getMessage() != null ? exception.getMessage() : "Access denied",
                request.getRequestURI(),
                null
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(ForbiddenAccessException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenAccess(
            ForbiddenAccessException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = buildErrorResponse(
                HttpStatus.FORBIDDEN,
                exception.getMessage(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(EventRequestValidationException.class)
    public ResponseEntity<ErrorResponse> handleEventRequestValidation(
            EventRequestValidationException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request.getRequestURI(),
                exception.getFieldErrors()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request.getRequestURI(),
                fieldErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
        Exception exception,
        HttpServletRequest request
    ) {
        log.error(
            "Unhandled exception for method={} path={}",
            request.getMethod(),
            request.getRequestURI(),
            exception
        );

        ErrorResponse response = buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Unexpected server error",
            request.getRequestURI(),
            null
        );

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(response);
    }

    private ErrorResponse buildErrorResponse(
            HttpStatus status,
            String message,
            String path,
            Map<String, String> fieldErrors
    ) {
        return new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                fieldErrors
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = buildErrorResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(InvalidGroupStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidGroupState(
            InvalidGroupStateException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = buildErrorResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(InvalidEventStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEventState(
            InvalidEventStateException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = buildErrorResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            org.springframework.web.server.ResponseStatusException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = buildErrorResponse(
                HttpStatus.valueOf(exception.getStatusCode().value()),
                exception.getReason(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity.status(exception.getStatusCode()).body(response);
    }

    @ExceptionHandler(InvalidMediaStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidMediaState(
        InvalidMediaStateException exception,
        HttpServletRequest request
    ) {
        ErrorResponse response = buildErrorResponse(
            HttpStatus.CONFLICT,
            exception.getMessage(),
            request.getRequestURI(),
            null
        );

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(response);
    }

    @ExceptionHandler(MediaStorageException.class)
    public ResponseEntity<ErrorResponse> handleMediaStorageFailure(
        MediaStorageException exception,
        HttpServletRequest request
    ) {
        log.error(
            "Media storage operation failed for path={}",
            request.getRequestURI(),
            exception
        );

        ErrorResponse response = buildErrorResponse(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Media storage is temporarily unavailable",
            request.getRequestURI(),
            null
        );

        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(response);
    }

}
