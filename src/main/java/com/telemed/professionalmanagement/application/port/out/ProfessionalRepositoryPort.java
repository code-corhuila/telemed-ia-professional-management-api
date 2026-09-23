package com.telemed.professionalmanagement.application.port.out;

import com.telemed.professionalmanagement.domain.Professional;
import java.util.List;
import java.util.Optional;

public interface ProfessionalRepositoryPort {
    Professional save(Professional professional);

    Optional<Professional> findById(Long professionalId);

    List<Professional> findAll();

    boolean existsByIdentityUserId(Long identityUserId);

    boolean existsByLicenseNumber(String licenseNumber);
}
