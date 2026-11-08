package co.edu.corhuila.telemedia.professionalmanagement.application.port.in;

import java.util.UUID;

import co.edu.corhuila.telemedia.professionalmanagement.domain.model.Professional;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.ProfessionalStatus;

public interface ChangeProfessionalStatusUseCase {

    /**
     * An administrator changes the profile status (FR-011, DEC-007); a workflow service client
     * may only deactivate it, as the compensation of the onboarding saga (DEC-015). Setting the
     * status the profile already has changes nothing.
     */
    Professional changeStatus(Caller caller, UUID professionalId, ProfessionalStatus status);
}
