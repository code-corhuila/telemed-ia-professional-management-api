package co.edu.corhuila.telemedia.professionalmanagement.adapter.out.persistence;

import java.util.UUID;

import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.Page;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.PageRequest;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.ProfessionalRepositoryPort;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.DuplicateProfessionalIdentityException;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.DuplicateProfessionalLicenseException;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.Professional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ProfessionalRepositoryAdapter implements ProfessionalRepositoryPort {

    /** DEC-010: from the most recently registered profile to the oldest; the id keeps pages stable. */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final ProfessionalJpaRepository professionalJpaRepository;
    private final ProfessionalPersistenceMapper mapper;

    public ProfessionalRepositoryAdapter(ProfessionalJpaRepository professionalJpaRepository,
                                        ProfessionalPersistenceMapper mapper) {
        this.professionalJpaRepository = professionalJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Professional save(Professional professional) {
        ProfessionalEntity entity = mapper.toEntity(professional);
        ProfessionalEntity saved;
        try {
            saved = professionalJpaRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            String constraintName = findConstraintName(exception);
            if ("uq_professionals_identity_user_id".equals(constraintName)) {
                throw new DuplicateProfessionalIdentityException(professional.getIdentityUserId());
            }
            if ("uq_professionals_license_number".equals(constraintName)) {
                throw new DuplicateProfessionalLicenseException(professional.getLicenseNumber());
            }
            throw exception;
        }
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Professional> findById(UUID professionalId) {
        return professionalJpaRepository.findById(professionalId).map(mapper::toDomain);
    }

    @Override
    public Page<Professional> findPage(PageRequest page) {
        org.springframework.data.domain.Page<ProfessionalEntity> found = professionalJpaRepository.findAll(
                org.springframework.data.domain.PageRequest.of(page.page() - 1, page.limit(), NEWEST_FIRST));
        return Page.of(found.getContent().stream().map(mapper::toDomain).toList(), page, found.getTotalElements());
    }

    @Override
    public boolean existsByIdentityUserId(UUID identityUserId) {
        return professionalJpaRepository.findByIdentityUserId(identityUserId).isPresent();
    }

    @Override
    public boolean existsByLicenseNumber(String licenseNumber) {
        if (licenseNumber == null) {
            return false;
        }
        return professionalJpaRepository.findByLicenseNumber(licenseNumber.trim()).isPresent();
    }

    private String findConstraintName(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violationException) {
                return violationException.getConstraintName();
            }
            cause = cause.getCause();
        }
        return null;
    }
}
