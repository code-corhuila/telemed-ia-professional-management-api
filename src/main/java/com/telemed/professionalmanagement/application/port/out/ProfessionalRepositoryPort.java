package com.telemed.professionalmanagement.application.port.out;

import java.util.UUID;

import com.telemed.professionalmanagement.domain.Professional;
import java.util.List;
import java.util.Optional;

public interface ProfessionalRepositoryPort {
    Professional save(Professional professional);

    Optional<Professional> findById(UUID professionalId);

    List<Professional> findAll();

    boolean existsByIdentityUserId(UUID identityUserId);

    boolean existsByLicenseNumber(String licenseNumber);
}
