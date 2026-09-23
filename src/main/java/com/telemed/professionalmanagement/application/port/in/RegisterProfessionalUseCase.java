package com.telemed.professionalmanagement.application.port.in;

import com.telemed.professionalmanagement.application.command.RegisterProfessionalCommand;
import com.telemed.professionalmanagement.domain.Professional;

public interface RegisterProfessionalUseCase {
    Professional register(RegisterProfessionalCommand command);
}
