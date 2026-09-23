package com.telemed.professionalmanagement.application.port.in;

import com.telemed.professionalmanagement.application.command.CreateSpecialtyCommand;
import com.telemed.professionalmanagement.domain.Specialty;

public interface CreateSpecialtyUseCase {
    Specialty create(CreateSpecialtyCommand command);
}
