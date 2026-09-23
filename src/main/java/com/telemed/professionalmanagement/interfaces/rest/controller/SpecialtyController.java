package com.telemed.professionalmanagement.interfaces.rest.controller;

import com.telemed.professionalmanagement.application.command.CreateSpecialtyCommand;
import com.telemed.professionalmanagement.application.command.UpdateSpecialtyCommand;
import com.telemed.professionalmanagement.application.port.in.CreateSpecialtyUseCase;
import com.telemed.professionalmanagement.application.port.in.ListSpecialtiesUseCase;
import com.telemed.professionalmanagement.application.port.in.UpdateSpecialtyUseCase;
import com.telemed.professionalmanagement.domain.Specialty;
import com.telemed.professionalmanagement.interfaces.rest.dto.SpecialtyRequest;
import com.telemed.professionalmanagement.interfaces.rest.dto.SpecialtyResponse;
import com.telemed.professionalmanagement.interfaces.rest.mapper.SpecialtyRestMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SpecialtyController {

    private final CreateSpecialtyUseCase createSpecialtyUseCase;
    private final UpdateSpecialtyUseCase updateSpecialtyUseCase;
    private final ListSpecialtiesUseCase listSpecialtiesUseCase;
    private final SpecialtyRestMapper specialtyRestMapper;

    public SpecialtyController(CreateSpecialtyUseCase createSpecialtyUseCase,
                              UpdateSpecialtyUseCase updateSpecialtyUseCase,
                              ListSpecialtiesUseCase listSpecialtiesUseCase,
                              SpecialtyRestMapper specialtyRestMapper) {
        this.createSpecialtyUseCase = createSpecialtyUseCase;
        this.updateSpecialtyUseCase = updateSpecialtyUseCase;
        this.listSpecialtiesUseCase = listSpecialtiesUseCase;
        this.specialtyRestMapper = specialtyRestMapper;
    }

    @GetMapping("/specialties")
    public ResponseEntity<List<SpecialtyResponse>> listSpecialties() {
        List<SpecialtyResponse> response = listSpecialtiesUseCase.listAll().stream()
                .map(specialtyRestMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/specialties")
    public ResponseEntity<SpecialtyResponse> createSpecialty(@Valid @RequestBody SpecialtyRequest request) {
        Specialty specialty = createSpecialtyUseCase.create(new CreateSpecialtyCommand(request.name(), request.description()));
        return ResponseEntity.status(HttpStatus.CREATED).body(specialtyRestMapper.toResponse(specialty));
    }

    @PutMapping("/specialties/{specialtyId}")
    public ResponseEntity<SpecialtyResponse> updateSpecialty(@PathVariable Long specialtyId,
                                                           @Valid @RequestBody SpecialtyRequest request) {
        Specialty specialty = updateSpecialtyUseCase.update(specialtyId, new UpdateSpecialtyCommand(request.name(), request.description()));
        return ResponseEntity.ok(specialtyRestMapper.toResponse(specialty));
    }
}
