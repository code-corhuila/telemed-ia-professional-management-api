package com.telemed.professionalmanagement.interfaces.rest.exception;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.telemed.professionalmanagement.application.port.in.AccessDeniedException;
import com.telemed.professionalmanagement.application.port.in.IdempotencyKeyReusedException;
import com.telemed.professionalmanagement.application.port.out.IdentityAccountConflictException;
import com.telemed.professionalmanagement.application.port.out.IdentityUnavailableException;
import com.telemed.professionalmanagement.domain.DuplicateProfessionalIdentityException;
import com.telemed.professionalmanagement.domain.DuplicateProfessionalLicenseException;
import com.telemed.professionalmanagement.domain.InvalidBusinessDataException;
import com.telemed.professionalmanagement.domain.ProfessionalNotFoundException;
import com.telemed.professionalmanagement.interfaces.rest.correlation.CorrelationFilter;
import com.telemed.professionalmanagement.interfaces.rest.exception.ApiError.FieldDetail;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * The single place where errors become HTTP responses (Anexo C). The domain throws typed
 * errors and does not know what a 409 is. Internal detail goes to the log only.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldDetail(error.getField(), error.getDefaultMessage()))
                .toList();
        return respond(400, ApiError.invalid(details, trace(request)));
    }

    @ExceptionHandler(InvalidRequestException.class)
    ResponseEntity<ApiError> invalidRequest(InvalidRequestException ex, HttpServletRequest request) {
        return respond(400, ApiError.invalid(ex.details(), trace(request)));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> invalidValue(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return invalidField(ex.getName(), "has an invalid format", request);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<ApiError> missingHeader(MissingRequestHeaderException ex, HttpServletRequest request) {
        return invalidField(ex.getHeaderName(), "header required", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiError> missingParameter(MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        return invalidField(ex.getParameterName(), "is required", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return respond(400, ApiError.of(ApiError.VALIDATION_ERROR,
                "the request body is not valid JSON", trace(request)));
    }

    @ExceptionHandler(InvalidBusinessDataException.class)
    ResponseEntity<ApiError> ruleViolation(InvalidBusinessDataException ex, HttpServletRequest request) {
        return respond(422, ApiError.of(ApiError.BUSINESS_RULE_VIOLATION, ex.getMessage(), trace(request)));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> accessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return respond(403, ApiError.forStatus(403, trace(request)));
    }

    @ExceptionHandler(ProfessionalNotFoundException.class)
    ResponseEntity<ApiError> notFound(RuntimeException ex, HttpServletRequest request) {
        return respond(404, ApiError.forStatus(404, trace(request)));
    }

    @ExceptionHandler({DuplicateProfessionalIdentityException.class, DuplicateProfessionalLicenseException.class,
            IdentityAccountConflictException.class})
    ResponseEntity<ApiError> duplicate(RuntimeException ex, HttpServletRequest request) {
        return respond(409, ApiError.of(ApiError.CONFLICT, ex.getMessage(), trace(request)));
    }

    @ExceptionHandler(IdempotencyKeyReusedException.class)
    ResponseEntity<ApiError> keyReused(IdempotencyKeyReusedException ex, HttpServletRequest request) {
        return respond(422, ApiError.of(ApiError.BUSINESS_RULE_VIOLATION, ex.getMessage(), trace(request)));
    }

    /** The identity service could not create the account; the reason goes to the log only. */
    @ExceptionHandler(IdentityUnavailableException.class)
    ResponseEntity<ApiError> identityUnavailable(IdentityUnavailableException ex, HttpServletRequest request) {
        LOG.warn("identity unavailable on {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return respond(503, ApiError.forStatus(503, trace(request)));
    }

    /** A uniqueness or integrity rule enforced by the database; the cause goes to the log. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        LOG.warn("integrity violation on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond(409, ApiError.forStatus(409, trace(request)));
    }

    /** Framework errors that already carry a status: unknown route, method, media type. */
    @ExceptionHandler({ServletException.class, ErrorResponseException.class})
    ResponseEntity<ApiError> framework(Exception ex, HttpServletRequest request) {
        if (!(ex instanceof ErrorResponse response) || response.getStatusCode().is5xxServerError()) {
            return unexpected(ex, request);
        }
        int status = response.getStatusCode().value();
        return ResponseEntity.status(status)
                .headers(response.getHeaders())
                .body(ApiError.forStatus(status, trace(request)));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest request) {
        LOG.error("unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond(500, ApiError.forStatus(500, trace(request)));
    }

    private ResponseEntity<ApiError> invalidField(String field, String message, HttpServletRequest request) {
        return respond(400, ApiError.invalid(List.of(new FieldDetail(field, message)), trace(request)));
    }

    private static ResponseEntity<ApiError> respond(int status, ApiError body) {
        return ResponseEntity.status(status).body(body);
    }

    private static String trace(HttpServletRequest request) {
        return CorrelationFilter.current(request);
    }
}
