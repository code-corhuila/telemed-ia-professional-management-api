package com.telemed.professionalmanagement.application.service;

import com.telemed.professionalmanagement.application.command.RegisterProfessionalCommand;
import com.telemed.professionalmanagement.application.port.in.GetProfessionalUseCase;
import com.telemed.professionalmanagement.application.port.in.ListProfessionalsUseCase;
import com.telemed.professionalmanagement.application.port.in.RegisterProfessionalUseCase;
import com.telemed.professionalmanagement.application.port.out.ProfessionalRepositoryPort;
import com.telemed.professionalmanagement.application.port.out.SpecialtyRepositoryPort;
import com.telemed.professionalmanagement.domain.DuplicateProfessionalIdentityException;
import com.telemed.professionalmanagement.domain.DuplicateProfessionalLicenseException;
import com.telemed.professionalmanagement.domain.Professional;
import com.telemed.professionalmanagement.domain.ProfessionalNotFoundException;
import com.telemed.professionalmanagement.domain.SpecialtyNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProfessionalApplicationService implements RegisterProfessionalUseCase, ListProfessionalsUseCase, GetProfessionalUseCase {

    private final ProfessionalRepositoryPort professionalRepositoryPort;
    private final SpecialtyRepositoryPort specialtyRepositoryPort;

    public ProfessionalApplicationService(ProfessionalRepositoryPort professionalRepositoryPort,
                                         SpecialtyRepositoryPort specialtyRepositoryPort) {
        this.professionalRepositoryPort = professionalRepositoryPort;
        this.specialtyRepositoryPort = specialtyRepositoryPort;
    }

    @Override
    public Professional register(RegisterProfessionalCommand command) {
        if (professionalRepositoryPort.existsByIdentityUserId(command.identityUserId())) {
            throw new DuplicateProfessionalIdentityException(command.identityUserId());
        }
        if (professionalRepositoryPort.existsByLicenseNumber(command.licenseNumber())) {
            throw new DuplicateProfessionalLicenseException(command.licenseNumber());
        }
        specialtyRepositoryPort.findById(command.specialtyId())
                .orElseThrow(() -> new SpecialtyNotFoundException(command.specialtyId()));

        Professional professional = new Professional(
                null,
                command.identityUserId(),
                command.licenseNumber(),
                command.specialtyId(),
                command.yearsExperience()
        );

        return professionalRepositoryPort.save(professional);
    }

    @Override
    public List<Professional> listAll() {
        return professionalRepositoryPort.findAll();
    }

    @Override
    public Professional getById(Long professionalId) {
        return professionalRepositoryPort.findById(professionalId)
                .orElseThrow(() -> new ProfessionalNotFoundException(professionalId));
    }
}
