package com.telemed.professionalmanagement.application.port.in;

import java.util.UUID;

public interface GetProfessionalUseCase {
    ProfessionalView getById(UUID professionalId);
}
