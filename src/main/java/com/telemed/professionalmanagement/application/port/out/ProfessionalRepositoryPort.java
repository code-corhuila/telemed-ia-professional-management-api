package com.telemed.professionalmanagement.application.port.out;

import java.util.UUID;

import com.telemed.professionalmanagement.application.port.in.Page;
import com.telemed.professionalmanagement.application.port.in.PageRequest;
import com.telemed.professionalmanagement.domain.Professional;
import java.util.Optional;

public interface ProfessionalRepositoryPort {
    Professional save(Professional professional);

    Optional<Professional> findById(UUID professionalId);

    /** Ordered by license number, so pages do not overlap. */
    Page<Professional> findPage(PageRequest page);

    boolean existsByIdentityUserId(UUID identityUserId);

    boolean existsByLicenseNumber(String licenseNumber);
}
