package com.telemed.professionalmanagement.adapters.persistence.mapper;

import com.telemed.professionalmanagement.adapters.persistence.entity.ProfessionalEntity;
import com.telemed.professionalmanagement.adapters.persistence.entity.SpecialtyEntity;
import com.telemed.professionalmanagement.domain.Professional;
import org.springframework.stereotype.Component;

@Component
public class ProfessionalPersistenceMapper {

    public Professional toDomain(ProfessionalEntity entity) {
        return new Professional(
                entity.getId(),
                entity.getIdentityUserId(),
                entity.getLicenseNumber(),
                entity.getSpecialty().getId(),
                entity.getYearsExperience()
        );
    }

    public ProfessionalEntity toEntity(Professional professional, SpecialtyEntity specialtyEntity) {
        return new ProfessionalEntity(
                professional.getId(),
                professional.getIdentityUserId(),
                professional.getLicenseNumber(),
                specialtyEntity,
                professional.getYearsExperience()
        );
    }
}
