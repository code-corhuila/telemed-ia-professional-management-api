package com.telemed.professionalmanagement.interfaces.rest.controller;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.telemed.professionalmanagement.application.command.ProfessionalRegistrationCommand;
import com.telemed.professionalmanagement.application.port.in.RegisterProfessionalAccountUseCase;
import com.telemed.professionalmanagement.application.port.in.Registration;
import com.telemed.professionalmanagement.interfaces.rest.auth.AuthFilter;
import com.telemed.professionalmanagement.interfaces.rest.dto.ProfessionalResponse;
import com.telemed.professionalmanagement.interfaces.rest.dto.RegisterProfessionalRequest;
import com.telemed.professionalmanagement.interfaces.rest.exception.ApiError.FieldDetail;
import com.telemed.professionalmanagement.interfaces.rest.exception.InvalidRequestException;
import com.telemed.professionalmanagement.interfaces.rest.mapper.ProfessionalRestMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validator;

/**
 * {@code POST /admin/register} of professional-service.yaml (Anexo C, numeral 5.3.8). The
 * {@code Idempotency-Key} header and the body are validated together, so one 400 names every
 * invalid field. The first registration answers 201 with {@code Location}; a retry, 200.
 */
@RestController
@RequestMapping(path = "/api/professionals", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProfessionalRegistrationController {

    static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    /** Public path of the gateway, the one clients use. */
    static final String PUBLIC_PATH = "/api/v1/professionals/";
    private static final int MIN_KEY_LENGTH = 8;
    private static final int MAX_KEY_LENGTH = 128;

    private final RegisterProfessionalAccountUseCase registration;
    private final ProfessionalRestMapper mapper;
    private final Validator validator;

    public ProfessionalRegistrationController(RegisterProfessionalAccountUseCase registration,
                                              ProfessionalRestMapper mapper, Validator validator) {
        this.registration = registration;
        this.mapper = mapper;
        this.validator = validator;
    }

    @PostMapping(path = "/admin/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProfessionalResponse> register(HttpServletRequest http,
            @RequestHeader(name = IDEMPOTENCY_KEY, required = false) String idempotencyKey,
            @RequestBody RegisterProfessionalRequest body) {
        List<FieldDetail> invalid = new ArrayList<>();
        if (idempotencyKey == null || idempotencyKey.length() < MIN_KEY_LENGTH
                || idempotencyKey.length() > MAX_KEY_LENGTH) {
            invalid.add(new FieldDetail(IDEMPOTENCY_KEY,
                    "header required, " + MIN_KEY_LENGTH + " to " + MAX_KEY_LENGTH + " characters"));
        }
        validator.validate(body).stream()
                .map(violation -> new FieldDetail(violation.getPropertyPath().toString(), violation.getMessage()))
                .sorted(Comparator.comparing(FieldDetail::field))
                .forEach(invalid::add);
        if (!invalid.isEmpty()) {
            throw new InvalidRequestException(invalid);
        }

        Registration result = registration.register(AuthFilter.currentCaller(http), idempotencyKey,
                new ProfessionalRegistrationCommand(body.fullName(), body.email(), body.identityDocument(),
                        body.password(), body.licenseNumber(), body.yearsExperience()));
        ProfessionalResponse response = mapper.toResponse(result.professional());
        if (result.created()) {
            return ResponseEntity.created(URI.create(PUBLIC_PATH + response.id())).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
