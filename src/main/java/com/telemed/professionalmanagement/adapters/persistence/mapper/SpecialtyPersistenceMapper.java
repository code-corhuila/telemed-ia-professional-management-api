package com.telemed.professionalmanagement.adapters.persistence.mapper;

import com.telemed.professionalmanagement.adapters.persistence.entity.SpecialtyEntity;
import com.telemed.professionalmanagement.domain.Specialty;
import org.springframework.stereotype.Component;

@Component
public class SpecialtyPersistenceMapper {

    public Specialty toDomain(SpecialtyEntity entity) {
        return new Specialty(entity.getId(), entity.getName(), entity.getDescription());
    }

    public SpecialtyEntity toEntity(Specialty specialty) {
        return new SpecialtyEntity(specialty.getId(), specialty.getName(), specialty.getDescription());
    }
}
