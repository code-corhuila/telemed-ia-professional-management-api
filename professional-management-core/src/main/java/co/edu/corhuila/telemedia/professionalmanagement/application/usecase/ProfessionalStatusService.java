package co.edu.corhuila.telemedia.professionalmanagement.application.usecase;

import java.util.Set;
import java.util.UUID;

import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.AccessDeniedException;
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
public class ProfessionalStatusService implements ChangeProfessionalStatusUseCase {

    private final ProfessionalRepositoryPort professionals;
    private final Set<String> workflowClients;

    /** Only administrators change the status. */
    public ProfessionalStatusService(ProfessionalRepositoryPort professionals) {
        this(professionals, Set.of());
    }

    /** Administrators, and the workflow clients only to deactivate (DEC-015). */
    public ProfessionalStatusService(ProfessionalRepositoryPort professionals, Set<String> workflowClients) {
        this.professionals = professionals;
        this.workflowClients = workflowClients == null ? Set.of() : Set.copyOf(workflowClients);
    }

    @Override
    public Professional changeStatus(Caller caller, UUID professionalId, ProfessionalStatus status) {
        authorize(caller, status);
        Professional current = professionals.findById(professionalId)
                .orElseThrow(() -> new ProfessionalNotFoundException(professionalId));
        if (current.getStatus() == status) {
            return current;
        }
        return professionals.save(current.withStatus(status));
    }

    /**
     * An administrator sets either status. A workflow client may only set {@code INACTIVE}: the
     * compensation of the onboarding saga (DEC-015). Decided before the profile is looked up.
     */
    private void authorize(Caller caller, ProfessionalStatus status) {
        if (caller.hasRole(Caller.ADMIN)) {
            return;
        }
        if (status == ProfessionalStatus.INACTIVE && caller.isServiceClientOf(workflowClients)) {
            return;
        }
        throw new AccessDeniedException();
    }
}
