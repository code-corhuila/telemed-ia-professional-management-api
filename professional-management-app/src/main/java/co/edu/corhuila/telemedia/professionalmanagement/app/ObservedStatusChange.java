package co.edu.corhuila.telemedia.professionalmanagement.app;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.AccessDeniedException;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.Caller;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.ChangeProfessionalStatusUseCase;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.DomainException;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.Professional;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.ProfessionalStatus;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * NFR-005: each status change (DEC-007) leaves one domain event in the log ({@code event},
 * {@code professional_id}, {@code status}, {@code reason}) and counts its outcome in
 * {@code professional_status_changes_total{outcome}}: applied, rejected or failed.
 */
public class ObservedStatusChange implements ChangeProfessionalStatusUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ObservedStatusChange.class);

    private final ChangeProfessionalStatusUseCase delegate;
    private final MeterRegistry meters;

    public ObservedStatusChange(ChangeProfessionalStatusUseCase delegate, MeterRegistry meters) {
        this.delegate = delegate;
        this.meters = meters;
    }

    @Override
    public Professional changeStatus(Caller caller, UUID professionalId, ProfessionalStatus status) {
        try {
            Professional professional = delegate.changeStatus(caller, professionalId, status);
            count("applied");
            LOG.atInfo().addKeyValue("event", "professional.status.changed")
                    .addKeyValue("professional_id", professional.getId().toString())
                    .addKeyValue("status", professional.getStatus().name())
                    .log("professional status changed");
            return professional;
        } catch (DomainException | AccessDeniedException e) {
            count("rejected");
            LOG.atInfo().addKeyValue("event", "professional.status.rejected")
                    .addKeyValue("professional_id", professionalId.toString())
                    .addKeyValue("reason", e.getClass().getSimpleName())
                    .log("professional status change rejected");
            throw e;
        } catch (RuntimeException e) {
            count("failed");
            LOG.atError().addKeyValue("event", "professional.status.failed")
                    .addKeyValue("professional_id", professionalId.toString())
                    .addKeyValue("reason", e.getClass().getSimpleName())
                    .log("professional status change failed");
            throw e;
        }
    }

    private void count(String outcome) {
        meters.counter("professional.status.changes", "outcome", outcome).increment();
    }
}
