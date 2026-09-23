package com.telemed.professionalmanagement.interfaces.rest.mapper;

import com.telemed.professionalmanagement.domain.Professional;
import com.telemed.professionalmanagement.interfaces.rest.dto.ProfessionalResponse;
import org.springframework.stereotype.Component;

@Component
public class ProfessionalRestMapper {

    public ProfessionalResponse toResponse(Professional professional) {
        return new ProfessionalResponse(
                professional.getId(),
                professional.getIdentityUserId(),
                professional.getLicenseNumber(),
                professional.getSpecialtyId(),
                professional.getYearsExperience()
        );
    }
}
