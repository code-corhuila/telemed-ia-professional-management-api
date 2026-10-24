package com.telemed.professionalmanagement.adapters.persistence.repository;

import java.util.UUID;

import com.telemed.professionalmanagement.adapters.persistence.entity.ProfessionalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfessionalJpaRepository extends JpaRepository<ProfessionalEntity, UUID> {
    Optional<ProfessionalEntity> findByIdentityUserId(UUID identityUserId);

    Optional<ProfessionalEntity> findByLicenseNumber(String licenseNumber);
}
