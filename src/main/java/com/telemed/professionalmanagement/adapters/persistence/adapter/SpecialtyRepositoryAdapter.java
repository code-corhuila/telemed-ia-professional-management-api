package com.telemed.professionalmanagement.adapters.persistence.adapter;

import com.telemed.professionalmanagement.adapters.persistence.entity.SpecialtyEntity;
import com.telemed.professionalmanagement.adapters.persistence.mapper.SpecialtyPersistenceMapper;
import com.telemed.professionalmanagement.adapters.persistence.repository.SpecialtyJpaRepository;
import com.telemed.professionalmanagement.application.port.out.SpecialtyRepositoryPort;
import com.telemed.professionalmanagement.domain.Specialty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class SpecialtyRepositoryAdapter implements SpecialtyRepositoryPort {

    private final SpecialtyJpaRepository specialtyJpaRepository;
    private final SpecialtyPersistenceMapper mapper;

    public SpecialtyRepositoryAdapter(SpecialtyJpaRepository specialtyJpaRepository, SpecialtyPersistenceMapper mapper) {
        this.specialtyJpaRepository = specialtyJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Specialty save(Specialty specialty) {
        SpecialtyEntity entity = mapper.toEntity(specialty);
        SpecialtyEntity saved = specialtyJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Specialty> findById(Long specialtyId) {
        return specialtyJpaRepository.findById(specialtyId).map(mapper::toDomain);
    }

    @Override
    public List<Specialty> findAll() {
        return specialtyJpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Specialty> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return specialtyJpaRepository.findByName(name.trim()).map(mapper::toDomain);
    }
}
