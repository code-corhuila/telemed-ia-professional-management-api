package com.telemed.professionalmanagement.application.service;

import java.util.UUID;

import com.telemed.professionalmanagement.application.port.in.GetProfessionalUseCase;
import com.telemed.professionalmanagement.application.port.in.ListProfessionalsUseCase;
import com.telemed.professionalmanagement.application.port.in.Page;
import com.telemed.professionalmanagement.application.port.in.PageRequest;
import com.telemed.professionalmanagement.application.port.out.ProfessionalRepositoryPort;
import com.telemed.professionalmanagement.domain.Professional;
import com.telemed.professionalmanagement.domain.ProfessionalNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ProfessionalApplicationService implements ListProfessionalsUseCase, GetProfessionalUseCase {

    private final ProfessionalRepositoryPort professionalRepositoryPort;
    public ProfessionalApplicationService(ProfessionalRepositoryPort professionalRepositoryPort) {
        this.professionalRepositoryPort = professionalRepositoryPort;
    }

    @Override
    public Page<Professional> list(PageRequest page) {
        return professionalRepositoryPort.findPage(page);
    }

    @Override
    public Professional getById(UUID professionalId) {
        return professionalRepositoryPort.findById(professionalId)
                .orElseThrow(() -> new ProfessionalNotFoundException(professionalId));
    }
}
