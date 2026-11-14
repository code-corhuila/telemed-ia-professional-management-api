package co.edu.corhuila.telemedia.professionalmanagement.application.port.in;

public interface CreateProfessionalProfileUseCase {

    /**
     * Checks only who calls, before the request is read, so a caller without permission gets
     * {@link AccessDeniedException} whatever it sends and never learns how a valid request
     * looks. The operation checks the caller again.
     */
    void authorize(Caller caller);

    /**
     * Step 2 of the onboarding saga (DEC-015): creates an active general practitioner profile
     * for an existing account, without calling Identity &amp; Access. Only a workflow service
     * client may call it. Repeating the same {@code idempotencyKey} with the same request
     * returns the first profile.
     */
    Registration create(Caller caller, String idempotencyKey, ProfessionalProfileCommand command);
}
