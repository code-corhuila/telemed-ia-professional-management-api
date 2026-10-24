package com.telemed.professionalmanagement.interfaces.rest.controller;

import java.util.UUID;

import com.telemed.professionalmanagement.application.port.in.GetProfessionalUseCase;
import com.telemed.professionalmanagement.application.port.in.ListProfessionalsUseCase;
import com.telemed.professionalmanagement.application.port.in.PageRequest;
import com.telemed.professionalmanagement.domain.Professional;
import com.telemed.professionalmanagement.interfaces.rest.dto.ProfessionalPageResponse;
import com.telemed.professionalmanagement.interfaces.rest.dto.ProfessionalResponse;
import com.telemed.professionalmanagement.interfaces.rest.exception.ApiError.FieldDetail;
import com.telemed.professionalmanagement.interfaces.rest.exception.InvalidRequestException;
import com.telemed.professionalmanagement.interfaces.rest.mapper.ProfessionalRestMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ProfessionalController {

    private final ListProfessionalsUseCase listProfessionalsUseCase;
    private final GetProfessionalUseCase getProfessionalUseCase;
    private final ProfessionalRestMapper professionalRestMapper;

    public ProfessionalController(ListProfessionalsUseCase listProfessionalsUseCase,
                                 GetProfessionalUseCase getProfessionalUseCase,
                                 ProfessionalRestMapper professionalRestMapper) {
        this.listProfessionalsUseCase = listProfessionalsUseCase;
        this.getProfessionalUseCase = getProfessionalUseCase;
        this.professionalRestMapper = professionalRestMapper;
    }

    @GetMapping("/professionals")
    public ResponseEntity<ProfessionalPageResponse> listProfessionals(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "" + PageRequest.DEFAULT_LIMIT) int limit) {
        List<FieldDetail> invalid = new ArrayList<>();
        if (page < 1) {
            invalid.add(new FieldDetail("page", "must be 1 or greater"));
        }
        if (limit < 1 || limit > PageRequest.MAX_LIMIT) {
            invalid.add(new FieldDetail("limit", "must be between 1 and " + PageRequest.MAX_LIMIT));
        }
        if (!invalid.isEmpty()) {
            throw new InvalidRequestException(invalid);
        }
        return ResponseEntity.ok(professionalRestMapper.toPageResponse(
                listProfessionalsUseCase.list(new PageRequest(page, limit))));
    }

    @GetMapping("/professionals/{professionalId}")
    public ResponseEntity<ProfessionalResponse> getProfessional(@PathVariable UUID professionalId) {
        Professional professional = getProfessionalUseCase.getById(professionalId);
        return ResponseEntity.ok(professionalRestMapper.toResponse(professional));
    }
}
