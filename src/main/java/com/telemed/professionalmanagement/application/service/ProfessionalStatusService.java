package com.telemed.professionalmanagement.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.telemed.professionalmanagement.application.port.in.Caller;
import com.telemed.professionalmanagement.application.port.in.ChangeProfessionalStatusUseCase;
import com.telemed.professionalmanagement.application.port.out.ProfessionalRepositoryPort;
import com.telemed.professionalmanagement.domain.Professional;
import com.telemed.professionalmanagement.domain.ProfessionalNotFoundException;
import com.telemed.professionalmanagement.domain.ProfessionalStatus;

/**
 * Changes the profile status (ADR-013). The account in Identity & Access and appointment
 * availability (ADR-009) are not touched.
 */
@Service
public class ProfessionalStatusService implements ChangeProfessionalStatusUseCase {

    private final ProfessionalRepositoryPort professionals;

    public ProfessionalStatusService(ProfessionalRepositoryPort professionals) {
        this.professionals = professionals;
    }

    @Override
    public Professional changeStatus(Caller caller, UUID professionalId, ProfessionalStatus status) {
        caller.requireAdmin();
        Professional current = professionals.findById(professionalId)
                .orElseThrow(() -> new ProfessionalNotFoundException(professionalId));
        if (current.getStatus() == status) {
            return current;
        }
        return professionals.save(current.withStatus(status));
    }
}
