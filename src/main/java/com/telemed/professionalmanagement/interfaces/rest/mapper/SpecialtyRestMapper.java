package com.telemed.professionalmanagement.interfaces.rest.mapper;

import com.telemed.professionalmanagement.domain.Specialty;
import com.telemed.professionalmanagement.interfaces.rest.dto.SpecialtyResponse;
import org.springframework.stereotype.Component;

@Component
public class SpecialtyRestMapper {

    public SpecialtyResponse toResponse(Specialty specialty) {
        return new SpecialtyResponse(specialty.getId(), specialty.getName(), specialty.getDescription());
    }
}
