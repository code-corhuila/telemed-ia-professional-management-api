package com.telemed.professionalmanagement.adapters.persistence.adapter;

import com.telemed.professionalmanagement.adapters.persistence.entity.ProfessionalEntity;
import com.telemed.professionalmanagement.adapters.persistence.entity.SpecialtyEntity;
import com.telemed.professionalmanagement.adapters.persistence.mapper.ProfessionalPersistenceMapper;
import com.telemed.professionalmanagement.adapters.persistence.repository.ProfessionalJpaRepository;
import com.telemed.professionalmanagement.adapters.persistence.repository.SpecialtyJpaRepository;
import com.telemed.professionalmanagement.application.port.out.ProfessionalRepositoryPort;
import com.telemed.professionalmanagement.domain.Professional;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ProfessionalRepositoryAdapter implements ProfessionalRepositoryPort {

    private final ProfessionalJpaRepository professionalJpaRepository;
    private final SpecialtyJpaRepository specialtyJpaRepository;
    private final ProfessionalPersistenceMapper mapper;

    public ProfessionalRepositoryAdapter(ProfessionalJpaRepository professionalJpaRepository,
                                        SpecialtyJpaRepository specialtyJpaRepository,
                                        ProfessionalPersistenceMapper mapper) {
        this.professionalJpaRepository = professionalJpaRepository;
        this.specialtyJpaRepository = specialtyJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Professional save(Professional professional) {
        SpecialtyEntity specialtyEntity = specialtyJpaRepository.findById(professional.getSpecialtyId())
                .orElseThrow(() -> new IllegalArgumentException("Specialty not found with id: " + professional.getSpecialtyId()));

        ProfessionalEntity entity = mapper.toEntity(professional, specialtyEntity);
        ProfessionalEntity saved = professionalJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Professional> findById(Long professionalId) {
        return professionalJpaRepository.findById(professionalId).map(mapper::toDomain);
    }

    @Override
    public List<Professional> findAll() {
        return professionalJpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByIdentityUserId(Long identityUserId) {
        return professionalJpaRepository.findByIdentityUserId(identityUserId).isPresent();
    }

    @Override
    public boolean existsByLicenseNumber(String licenseNumber) {
        if (licenseNumber == null) {
            return false;
        }
        return professionalJpaRepository.findByLicenseNumber(licenseNumber.trim()).isPresent();
    }
}
