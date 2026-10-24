package com.telemed.professionalmanagement.application.port.in;

import java.util.UUID;

import com.telemed.professionalmanagement.domain.Professional;
import com.telemed.professionalmanagement.domain.ProfessionalStatus;

public interface ChangeProfessionalStatusUseCase {

    /**
     * Only an administrator changes the profile status (FR-011, DEC-007). Setting the status
     * the profile already has changes nothing.
     */
    Professional changeStatus(Caller caller, UUID professionalId, ProfessionalStatus status);
}
