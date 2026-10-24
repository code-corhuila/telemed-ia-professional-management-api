package com.telemed.professionalmanagement.application.port.in;

import com.telemed.professionalmanagement.application.command.ProfessionalRegistrationCommand;

public interface RegisterProfessionalAccountUseCase {

    /**
     * Only an administrator registers a professional (DEC-003): the account in the identity
     * domain and an active general practitioner profile. Repeating the same
     * {@code idempotencyKey} with the same request returns the first registration.
     */
    Registration register(Caller caller, String idempotencyKey, ProfessionalRegistrationCommand command);
}
