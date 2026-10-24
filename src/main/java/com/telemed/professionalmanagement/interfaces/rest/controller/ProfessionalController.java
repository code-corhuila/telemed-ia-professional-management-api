package com.telemed.professionalmanagement.interfaces.rest.controller;

import java.util.UUID;

import com.telemed.professionalmanagement.application.command.RegisterProfessionalCommand;
import com.telemed.professionalmanagement.application.port.in.GetProfessionalUseCase;
import com.telemed.professionalmanagement.application.port.in.ListProfessionalsUseCase;
import com.telemed.professionalmanagement.application.port.in.RegisterProfessionalUseCase;
import com.telemed.professionalmanagement.domain.Professional;
import com.telemed.professionalmanagement.interfaces.rest.auth.AuthFilter;
import com.telemed.professionalmanagement.interfaces.rest.dto.ProfessionalRequest;
import com.telemed.professionalmanagement.interfaces.rest.dto.ProfessionalResponse;
import com.telemed.professionalmanagement.interfaces.rest.mapper.ProfessionalRestMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProfessionalController {

    private final RegisterProfessionalUseCase registerProfessionalUseCase;
    private final ListProfessionalsUseCase listProfessionalsUseCase;
    private final GetProfessionalUseCase getProfessionalUseCase;
    private final ProfessionalRestMapper professionalRestMapper;

    public ProfessionalController(RegisterProfessionalUseCase registerProfessionalUseCase,
                                 ListProfessionalsUseCase listProfessionalsUseCase,
                                 GetProfessionalUseCase getProfessionalUseCase,
                                 ProfessionalRestMapper professionalRestMapper) {
        this.registerProfessionalUseCase = registerProfessionalUseCase;
        this.listProfessionalsUseCase = listProfessionalsUseCase;
        this.getProfessionalUseCase = getProfessionalUseCase;
        this.professionalRestMapper = professionalRestMapper;
    }

    @GetMapping("/professionals")
    public ResponseEntity<List<ProfessionalResponse>> listProfessionals() {
        List<ProfessionalResponse> response = listProfessionalsUseCase.listAll().stream()
                .map(professionalRestMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/professionals/{professionalId}")
    public ResponseEntity<ProfessionalResponse> getProfessional(@PathVariable UUID professionalId) {
        Professional professional = getProfessionalUseCase.getById(professionalId);
        return ResponseEntity.ok(professionalRestMapper.toResponse(professional));
    }

    @PostMapping("/professionals")
    public ResponseEntity<ProfessionalResponse> registerProfessional(@Valid @RequestBody ProfessionalRequest request,
                                                                     HttpServletRequest http) {
        Professional professional = registerProfessionalUseCase.register(AuthFilter.currentCaller(http), new RegisterProfessionalCommand(
                request.identityUserId(),
                request.licenseNumber(),
                request.yearsExperience()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(professionalRestMapper.toResponse(professional));
    }
}
