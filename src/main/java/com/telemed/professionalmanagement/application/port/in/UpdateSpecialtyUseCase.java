package com.telemed.professionalmanagement.application.port.in;

import com.telemed.professionalmanagement.application.command.UpdateSpecialtyCommand;
import com.telemed.professionalmanagement.domain.Specialty;

public interface UpdateSpecialtyUseCase {
    Specialty update(Long specialtyId, UpdateSpecialtyCommand command);
}
