package co.edu.corhuila.telemedia.professionalmanagement.application.port.out;

import java.util.UUID;

/** The identity domain, reached through its API: this service never writes its data. */
public interface IdentityAccountPort {

    /**
     * Creates the account and returns its id. The same {@code idempotencyKey} is passed on, so
     * a retry does not create a second account.
     *
     * @throws IdentityAccountConflictException when the e-mail or the document is already registered
     * @throws IdentityUnavailableException when the identity service cannot create it now
     */
    UUID createProfessionalAccount(NewProfessionalAccount account, String idempotencyKey);
}
