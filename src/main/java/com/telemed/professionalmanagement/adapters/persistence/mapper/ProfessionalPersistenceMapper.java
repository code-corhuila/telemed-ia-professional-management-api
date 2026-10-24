package com.telemed.professionalmanagement.adapters.persistence.mapper;

import com.telemed.professionalmanagement.adapters.persistence.entity.ProfessionalEntity;
import com.telemed.professionalmanagement.domain.Professional;
import com.telemed.professionalmanagement.domain.ProfessionalStatus;
import com.telemed.professionalmanagement.domain.ProfessionalType;
import org.springframework.stereotype.Component;

@Component
public class ProfessionalPersistenceMapper {

    public Professional toDomain(ProfessionalEntity entity) {
        return new Professional(
                entity.getId(),
                entity.getIdentityUserId(),
                entity.getLicenseNumber(),
                entity.getYearsExperience(),
                ProfessionalType.valueOf(entity.getProfessionalType()),
                ProfessionalStatus.valueOf(entity.getStatus())
        );
    }

    public ProfessionalEntity toEntity(Professional professional) {
        return new ProfessionalEntity(
                professional.getId(),
                professional.getIdentityUserId(),
                professional.getLicenseNumber(),
                professional.getYearsExperience(),
                professional.getProfessionalType().name(),
                professional.getStatus().name()
        );
    }
}
