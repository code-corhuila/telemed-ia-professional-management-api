package com.telemed.professionalmanagement.application.port.in;

import java.util.UUID;

import com.telemed.professionalmanagement.domain.Professional;

public interface GetProfessionalUseCase {
    Professional getById(UUID professionalId);
}
