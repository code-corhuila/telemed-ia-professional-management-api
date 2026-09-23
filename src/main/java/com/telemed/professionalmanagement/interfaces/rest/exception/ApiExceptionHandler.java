package com.telemed.professionalmanagement.interfaces.rest.exception;

import com.telemed.professionalmanagement.domain.DuplicateProfessionalIdentityException;
import com.telemed.professionalmanagement.domain.DuplicateProfessionalLicenseException;
import com.telemed.professionalmanagement.domain.DuplicateSpecialtyNameException;
import com.telemed.professionalmanagement.domain.InvalidBusinessDataException;
import com.telemed.professionalmanagement.domain.ProfessionalNotFoundException;
import com.telemed.professionalmanagement.domain.SpecialtyNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return buildError(HttpStatus.BAD_REQUEST, "Validation failed", message, request.getRequestURI());
    }

    @ExceptionHandler(InvalidBusinessDataException.class)
    public ResponseEntity<ApiError> handleInvalidBusinessData(InvalidBusinessDataException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "Invalid business data", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ProfessionalNotFoundException.class)
    public ResponseEntity<ApiError> handleProfessionalNotFound(ProfessionalNotFoundException ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, "Professional not found", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(SpecialtyNotFoundException.class)
    public ResponseEntity<ApiError> handleSpecialtyNotFound(SpecialtyNotFoundException ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, "Specialty not found", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({DuplicateProfessionalIdentityException.class, DuplicateProfessionalLicenseException.class, DuplicateSpecialtyNameException.class})
    public ResponseEntity<ApiError> handleConflict(RuntimeException ex, HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, "Database constraint violation", "The operation violates a database uniqueness or integrity rule.", request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex, HttpServletRequest request) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "An unexpected error occurred.", request.getRequestURI());
    }

    private ResponseEntity<ApiError> buildError(HttpStatus status, String error, String message, String path) {
        ApiError apiError = new ApiError(Instant.now(), status.value(), error, message, path);
        return ResponseEntity.status(status).body(apiError);
    }
}
