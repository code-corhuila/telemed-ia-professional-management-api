package co.edu.corhuila.telemedia.professionalmanagement.application.port.out;

import java.util.UUID;

import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.Page;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.PageRequest;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.Professional;
import java.util.Optional;

public interface ProfessionalRepositoryPort {
    Professional save(Professional professional);

    Optional<Professional> findById(UUID professionalId);

    /** Ordered by license number, so pages do not overlap. */
    Page<Professional> findPage(PageRequest page);

    boolean existsByIdentityUserId(UUID identityUserId);

    boolean existsByLicenseNumber(String licenseNumber);
}
