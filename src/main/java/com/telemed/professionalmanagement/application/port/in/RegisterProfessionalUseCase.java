package com.telemed.professionalmanagement.application.port.in;

import com.telemed.professionalmanagement.application.command.RegisterProfessionalCommand;
import com.telemed.professionalmanagement.domain.Professional;

public interface RegisterProfessionalUseCase {
    /** Only an administrator registers a professional (DEC-003). */
    Professional register(Caller caller, RegisterProfessionalCommand command);
}
