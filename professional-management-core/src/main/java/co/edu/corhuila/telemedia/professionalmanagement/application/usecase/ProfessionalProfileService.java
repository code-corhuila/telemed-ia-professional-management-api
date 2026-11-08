package co.edu.corhuila.telemedia.professionalmanagement.application.usecase;

import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.AccessDeniedException;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.Caller;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.CreateProfessionalProfileUseCase;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.ProfessionalProfileCommand;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.Registration;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.ProfessionalRegistrationStore;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.StoredRegistration;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.DuplicateProfessionalIdentityException;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.DuplicateProfessionalLicenseException;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.InvalidBusinessDataException;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.Professional;
import java.util.Optional;
import java.util.Set;

/**
 * Creates the profile of an account that Identity &amp; Access already created: step 2 of the
 * onboarding saga of {@code telemed-ia-workflow} (DEC-015). The profile and its idempotency key
 * are stored in one transaction, with the same replay rules as {@code /admin/register}.
 */
public class ProfessionalProfileService implements CreateProfessionalProfileUseCase {

    private final ProfessionalRegistrationStore store;
    private final Set<String> workflowClients;

    public ProfessionalProfileService(ProfessionalRegistrationStore store, Set<String> workflowClients) {
        this.store = store;
        this.workflowClients = workflowClients == null ? Set.of() : Set.copyOf(workflowClients);
    }

    @Override
    public Registration create(Caller caller, String idempotencyKey, ProfessionalProfileCommand command) {
        if (!caller.isServiceClientOf(workflowClients)) {
            throw new AccessDeniedException();
        }
        if (command.accountId() == null) {
            throw new InvalidBusinessDataException("accountId is required");
        }
        Professional.checkProfile(command.licenseNumber(), command.yearsExperience());
        String requestHash = requestHash(command);
        Optional<StoredRegistration> previous = store.findByIdempotencyKey(idempotencyKey);
        if (previous.isPresent()) {
            return RegistrationReplays.replay(previous.get(), requestHash);
        }
        String license = command.licenseNumber().trim();
        if (store.existsByLicenseNumber(license)) {
            return RegistrationReplays.replayOrThrow(store, idempotencyKey, requestHash,
                    new DuplicateProfessionalLicenseException(license));
        }
        Optional<Professional> saved;
        try {
            saved = store.saveRegistration(Professional.register(command.accountId(), license,
                    command.yearsExperience()), idempotencyKey, requestHash);
        } catch (DuplicateProfessionalLicenseException | DuplicateProfessionalIdentityException duplicate) {
            return RegistrationReplays.replayOrThrow(store, idempotencyKey, requestHash, duplicate);
        }
        return saved.map(professional -> new Registration(professional, true))
                .orElseGet(() -> RegistrationReplays.replay(
                        store.findByIdempotencyKey(idempotencyKey).orElseThrow(), requestHash));
    }

    /**
     * What identifies the request behind a key. The {@code profile} prefix keeps it apart from
     * the hash of an administrative registration that reused the same key.
     */
    static String requestHash(ProfessionalProfileCommand command) {
        return RegistrationReplays.sha256(String.join("\n", "profile",
                String.valueOf(command.accountId()),
                command.licenseNumber() == null ? "" : command.licenseNumber().trim(),
                String.valueOf(command.yearsExperience())));
    }
}
