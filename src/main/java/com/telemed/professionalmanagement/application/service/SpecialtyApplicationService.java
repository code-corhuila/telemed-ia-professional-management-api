package com.telemed.professionalmanagement.application.service;

import com.telemed.professionalmanagement.application.command.CreateSpecialtyCommand;
import com.telemed.professionalmanagement.application.command.UpdateSpecialtyCommand;
import com.telemed.professionalmanagement.application.port.in.CreateSpecialtyUseCase;
import com.telemed.professionalmanagement.application.port.in.ListSpecialtiesUseCase;
import com.telemed.professionalmanagement.application.port.in.UpdateSpecialtyUseCase;
import com.telemed.professionalmanagement.application.port.out.SpecialtyRepositoryPort;
import com.telemed.professionalmanagement.domain.DuplicateSpecialtyNameException;
import com.telemed.professionalmanagement.domain.Specialty;
import com.telemed.professionalmanagement.domain.SpecialtyNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SpecialtyApplicationService implements CreateSpecialtyUseCase, UpdateSpecialtyUseCase, ListSpecialtiesUseCase {

    private final SpecialtyRepositoryPort specialtyRepositoryPort;

    public SpecialtyApplicationService(SpecialtyRepositoryPort specialtyRepositoryPort) {
        this.specialtyRepositoryPort = specialtyRepositoryPort;
    }

    @Override
    public Specialty create(CreateSpecialtyCommand command) {
        specialtyRepositoryPort.findByName(command.name())
                .ifPresent(existing -> {
                    throw new DuplicateSpecialtyNameException(existing.getName());
                });

        Specialty specialty = new Specialty(null, command.name(), command.description());
        return specialtyRepositoryPort.save(specialty);
    }

    @Override
    public Specialty update(Long specialtyId, UpdateSpecialtyCommand command) {
        Specialty existing = specialtyRepositoryPort.findById(specialtyId)
                .orElseThrow(() -> new SpecialtyNotFoundException(specialtyId));

        specialtyRepositoryPort.findByName(command.name())
                .filter(found -> !found.getId().equals(specialtyId))
                .ifPresent(found -> {
                    throw new DuplicateSpecialtyNameException(found.getName());
                });

        Specialty updated = existing.update(command.name(), command.description());
        return specialtyRepositoryPort.save(updated);
    }

    @Override
    public List<Specialty> listAll() {
        return specialtyRepositoryPort.findAll();
    }
}
