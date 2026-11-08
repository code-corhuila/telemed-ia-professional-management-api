package co.edu.corhuila.telemedia.professionalmanagement.application.port.in;

public interface CreateProfessionalProfileUseCase {

    /**
     * Step 2 of the onboarding saga (DEC-015): creates an active general practitioner profile
     * for an existing account, without calling Identity &amp; Access. Only a workflow service
     * client may call it. Repeating the same {@code idempotencyKey} with the same request
     * returns the first profile.
     */
    Registration create(Caller caller, String idempotencyKey, ProfessionalProfileCommand command);
}
