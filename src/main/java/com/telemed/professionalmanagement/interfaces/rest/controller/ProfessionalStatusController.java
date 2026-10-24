package com.telemed.professionalmanagement.interfaces.rest.controller;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.telemed.professionalmanagement.application.port.in.ChangeProfessionalStatusUseCase;
import com.telemed.professionalmanagement.domain.ProfessionalStatus;
import com.telemed.professionalmanagement.interfaces.rest.auth.AuthFilter;
import com.telemed.professionalmanagement.interfaces.rest.dto.ProfessionalResponse;
import com.telemed.professionalmanagement.interfaces.rest.exception.ApiError.FieldDetail;
import com.telemed.professionalmanagement.interfaces.rest.exception.InvalidRequestException;
import com.telemed.professionalmanagement.interfaces.rest.mapper.ProfessionalRestMapper;

import jakarta.servlet.http.HttpServletRequest;

/**
 * {@code PATCH /{id}/status} of professional-service.yaml (DEC-007). The body is closed
 * ({@code additionalProperties: false}): only {@code status}, {@code ACTIVE} or
 * {@code INACTIVE}. It is read as a tree so unknown fields are rejected here.
 */
@RestController
@RequestMapping(path = "/api/professionals", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProfessionalStatusController {

    private static final String STATUS = "status";

    private final ChangeProfessionalStatusUseCase statusChange;
    private final ProfessionalRestMapper mapper;

    public ProfessionalStatusController(ChangeProfessionalStatusUseCase statusChange, ProfessionalRestMapper mapper) {
        this.statusChange = statusChange;
        this.mapper = mapper;
    }

    @PatchMapping(path = "/{professionalId}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProfessionalResponse> changeStatus(HttpServletRequest http,
            @PathVariable UUID professionalId, @RequestBody JsonNode body) {
        List<FieldDetail> invalid = new ArrayList<>();
        ProfessionalStatus status = null;
        if (body == null || !body.isObject()) {
            invalid.add(new FieldDetail("body", "must be a JSON object"));
        } else {
            for (Iterator<String> names = body.fieldNames(); names.hasNext();) {
                String name = names.next();
                if (!STATUS.equals(name)) {
                    invalid.add(new FieldDetail(name, "is not allowed"));
                }
            }
            status = status(body.get(STATUS), invalid);
        }
        if (!invalid.isEmpty()) {
            throw new InvalidRequestException(invalid);
        }
        return ResponseEntity.ok(mapper.toResponse(
                statusChange.changeStatus(AuthFilter.currentCaller(http), professionalId, status)));
    }

    private static ProfessionalStatus status(JsonNode value, List<FieldDetail> invalid) {
        if (value != null && value.isTextual()) {
            for (ProfessionalStatus candidate : ProfessionalStatus.values()) {
                if (candidate.name().equals(value.asText())) {
                    return candidate;
                }
            }
        }
        invalid.add(new FieldDetail(STATUS, "must be ACTIVE or INACTIVE"));
        return null;
    }
}
