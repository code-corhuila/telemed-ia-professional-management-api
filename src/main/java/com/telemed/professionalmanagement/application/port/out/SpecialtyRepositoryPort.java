package com.telemed.professionalmanagement.application.port.out;

import com.telemed.professionalmanagement.domain.Specialty;
import java.util.List;
import java.util.Optional;

public interface SpecialtyRepositoryPort {
    Specialty save(Specialty specialty);

    Optional<Specialty> findById(Long specialtyId);

    List<Specialty> findAll();

    Optional<Specialty> findByName(String name);
}
