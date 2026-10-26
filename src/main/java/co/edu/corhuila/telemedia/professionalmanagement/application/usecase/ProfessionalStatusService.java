package co.edu.corhuila.telemedia.professionalmanagement.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.Caller;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.ChangeProfessionalStatusUseCase;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.ProfessionalRepositoryPort;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.Professional;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.ProfessionalNotFoundException;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.ProfessionalStatus;

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
