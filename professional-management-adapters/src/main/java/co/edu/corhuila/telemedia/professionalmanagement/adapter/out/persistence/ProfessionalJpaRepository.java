package co.edu.corhuila.telemedia.professionalmanagement.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfessionalJpaRepository extends JpaRepository<ProfessionalEntity, UUID> {
    Optional<ProfessionalEntity> findByIdentityUserId(UUID identityUserId);

    Optional<ProfessionalEntity> findByLicenseNumber(String licenseNumber);
}
