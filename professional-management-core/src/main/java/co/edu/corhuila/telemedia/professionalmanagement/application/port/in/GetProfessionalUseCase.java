package co.edu.corhuila.telemedia.professionalmanagement.application.port.in;

import java.util.UUID;

public interface GetProfessionalUseCase {
    /** Any authenticated caller; the account e-mail only for an administrator (DEC-009). */
    ProfessionalView getById(Caller caller, UUID professionalId);
}
