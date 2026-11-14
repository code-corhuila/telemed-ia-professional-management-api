package co.edu.corhuila.telemedia.professionalmanagement.application.port.in;


public interface RegisterProfessionalAccountUseCase {

    /**
     * Checks only who calls, before the request is read, so a caller without permission gets
     * {@link AccessDeniedException} whatever it sends and never learns how a valid request
     * looks. The operation checks the caller again.
     */
    void authorize(Caller caller);

    /**
     * Only an administrator registers a professional (DEC-003): the account in the identity
     * domain and an active general practitioner profile. Repeating the same
     * {@code idempotencyKey} with the same request returns the first registration.
     */
    Registration register(Caller caller, String idempotencyKey, ProfessionalRegistrationCommand command);
}
