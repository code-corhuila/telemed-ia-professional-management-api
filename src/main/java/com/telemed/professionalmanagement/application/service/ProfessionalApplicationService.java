package com.telemed.professionalmanagement.application.service;

import com.telemed.professionalmanagement.application.command.RegisterProfessionalCommand;
import com.telemed.professionalmanagement.application.port.in.Caller;
import com.telemed.professionalmanagement.application.port.in.GetProfessionalUseCase;
import com.telemed.professionalmanagement.application.port.in.ListProfessionalsUseCase;
import com.telemed.professionalmanagement.application.port.in.RegisterProfessionalUseCase;
import com.telemed.professionalmanagement.application.port.out.ProfessionalRepositoryPort;
import com.telemed.professionalmanagement.domain.DuplicateProfessionalIdentityException;
import com.telemed.professionalmanagement.domain.DuplicateProfessionalLicenseException;
import com.telemed.professionalmanagement.domain.Professional;
import com.telemed.professionalmanagement.domain.ProfessionalNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProfessionalApplicationService implements RegisterProfessionalUseCase, ListProfessionalsUseCase, GetProfessionalUseCase {

    private final ProfessionalRepositoryPort professionalRepositoryPort;
    public ProfessionalApplicationService(ProfessionalRepositoryPort professionalRepositoryPort) {
        this.professionalRepositoryPort = professionalRepositoryPort;
    }

    @Override
    public Professional register(Caller caller, RegisterProfessionalCommand command) {
        caller.requireAdmin();
        if (professionalRepositoryPort.existsByIdentityUserId(command.identityUserId())) {
            throw new DuplicateProfessionalIdentityException(command.identityUserId());
        }
        if (professionalRepositoryPort.existsByLicenseNumber(command.licenseNumber())) {
            throw new DuplicateProfessionalLicenseException(command.licenseNumber());
        }
        return professionalRepositoryPort.save(Professional.register(
                command.identityUserId(),
                command.licenseNumber(),
                command.yearsExperience()
        ));
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
