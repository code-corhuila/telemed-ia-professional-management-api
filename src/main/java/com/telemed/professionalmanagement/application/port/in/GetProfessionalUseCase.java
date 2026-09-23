package com.telemed.professionalmanagement.application.port.in;

import com.telemed.professionalmanagement.domain.Professional;

public interface GetProfessionalUseCase {
    Professional getById(Long professionalId);
}
