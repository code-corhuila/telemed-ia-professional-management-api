package co.edu.corhuila.telemedia.professionalmanagement.application.usecase;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.Caller;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.ProfessionalRegistrationCommand;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.RegisterProfessionalAccountUseCase;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.Registration;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.IdentityAccountPort;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.NewProfessionalAccount;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.ProfessionalRegistrationStore;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.StoredRegistration;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.DuplicateProfessionalIdentityException;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.DuplicateProfessionalLicenseException;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.Professional;

/**
 * Registers a professional (DEC-003): the account in the identity domain, then the profile
 * here. The idempotency key is checked before the account is created, so a retry never asks
 * identity for a second account; the profile and the key are stored in one transaction.
 */
public class ProfessionalRegistrationService implements RegisterProfessionalAccountUseCase {

    private final IdentityAccountPort identityAccounts;
    private final ProfessionalRegistrationStore store;

    public ProfessionalRegistrationService(IdentityAccountPort identityAccounts, ProfessionalRegistrationStore store) {
        this.identityAccounts = identityAccounts;
        this.store = store;
    }

    @Override
    public Registration register(Caller caller, String idempotencyKey, ProfessionalRegistrationCommand command) {
        caller.requireAdmin();
        Professional.checkProfile(command.licenseNumber(), command.yearsExperience());
        String requestHash = requestHash(command);

        Optional<StoredRegistration> previous = store.findByIdempotencyKey(idempotencyKey);
        if (previous.isPresent()) {
            return RegistrationReplays.replay(previous.get(), requestHash);
        }
        if (store.existsByLicenseNumber(command.licenseNumber())) {
            return RegistrationReplays.replayOrThrow(store, idempotencyKey, requestHash,
                    new DuplicateProfessionalLicenseException(command.licenseNumber().trim()));
        }

        UUID accountId = identityAccounts.createProfessionalAccount(new NewProfessionalAccount(
                command.fullName(), command.email(), command.identityDocument(), command.password()), idempotencyKey);

        Optional<Professional> saved;
        try {
            saved = store.saveRegistration(Professional.register(accountId, command.licenseNumber(),
                    command.yearsExperience()), idempotencyKey, requestHash);
        } catch (DuplicateProfessionalLicenseException | DuplicateProfessionalIdentityException duplicate) {
            return RegistrationReplays.replayOrThrow(store, idempotencyKey, requestHash, duplicate);
        }
        return saved.map(professional -> new Registration(professional, true))
                .orElseGet(() -> RegistrationReplays.replay(
                        store.findByIdempotencyKey(idempotencyKey).orElseThrow(), requestHash));
    }

    /**
     * What identifies the request behind a key. The password is left out on purpose, so no
     * value derived from it is ever stored.
     */
    static String requestHash(ProfessionalRegistrationCommand command) {
        String canonical = String.join("\n",
                trim(command.fullName()),
                trim(command.email()).toLowerCase(Locale.ROOT),
                trim(command.identityDocument()),
                trim(command.licenseNumber()),
                String.valueOf(command.yearsExperience()));
        return RegistrationReplays.sha256(canonical);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
