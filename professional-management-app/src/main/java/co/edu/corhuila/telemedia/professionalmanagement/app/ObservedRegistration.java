package co.edu.corhuila.telemedia.professionalmanagement.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.AccessDeniedException;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.Caller;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.IdempotencyKeyReusedException;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.ProfessionalRegistrationCommand;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.RegisterProfessionalAccountUseCase;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.Registration;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.IdentityAccountConflictException;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.IdentityAccountRejectedException;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.IdentityUnavailableException;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.DomainException;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * NFR-005: each registration leaves one domain event in the log ({@code event},
 * {@code professional_id}, {@code duration_ms}, {@code reason}) and counts its outcome in
 * {@code professional_registrations_total{outcome}}: created, replayed, rejected (the caller's
 * mistake), unavailable (identity did not answer) or failed. Nothing of the request is logged:
 * names, e-mail, document, password and key are personal data or secrets.
 */
public class ObservedRegistration implements RegisterProfessionalAccountUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ObservedRegistration.class);

    private final RegisterProfessionalAccountUseCase delegate;
    private final MeterRegistry meters;

    public ObservedRegistration(RegisterProfessionalAccountUseCase delegate, MeterRegistry meters) {
        this.delegate = delegate;
        this.meters = meters;
    }

    @Override
    public Registration register(Caller caller, String idempotencyKey, ProfessionalRegistrationCommand command) {
        long start = System.nanoTime();
        try {
            Registration registration = delegate.register(caller, idempotencyKey, command);
            String outcome = registration.created() ? "created" : "replayed";
            count(outcome);
            LOG.atInfo().addKeyValue("event", "professional.registration." + outcome)
                    .addKeyValue("professional_id", registration.professional().getId().toString())
                    .addKeyValue("duration_ms", millisSince(start))
                    .log("professional registration {}", outcome);
            return registration;
        } catch (RuntimeException e) {
            String outcome = outcomeOf(e);
            count(outcome);
            Level level = switch (outcome) {
                case "rejected" -> Level.INFO;
                case "unavailable" -> Level.WARN;
                default -> Level.ERROR;
            };
            LOG.atLevel(level).addKeyValue("event", "professional.registration." + outcome)
                    .addKeyValue("reason", e.getClass().getSimpleName())
                    .addKeyValue("duration_ms", millisSince(start))
                    .log("professional registration {}", outcome);
            throw e;
        }
    }

    private static String outcomeOf(RuntimeException e) {
        if (e instanceof IdentityUnavailableException) {
            return "unavailable";
        }
        if (e instanceof DomainException || e instanceof AccessDeniedException
                || e instanceof IdempotencyKeyReusedException || e instanceof IdentityAccountConflictException
                || e instanceof IdentityAccountRejectedException) {
            return "rejected";
        }
        return "failed";
    }

    private void count(String outcome) {
        meters.counter("professional.registrations", "outcome", outcome).increment();
    }

    private static long millisSince(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }
}
