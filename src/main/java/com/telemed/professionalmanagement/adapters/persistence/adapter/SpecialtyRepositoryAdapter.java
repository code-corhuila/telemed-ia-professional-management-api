package com.telemed.professionalmanagement.adapters.persistence.adapter;

import com.telemed.professionalmanagement.adapters.persistence.entity.SpecialtyEntity;
import com.telemed.professionalmanagement.adapters.persistence.mapper.SpecialtyPersistenceMapper;
import com.telemed.professionalmanagement.adapters.persistence.repository.SpecialtyJpaRepository;
import com.telemed.professionalmanagement.application.port.out.SpecialtyRepositoryPort;
import com.telemed.professionalmanagement.domain.DuplicateSpecialtyNameException;
import com.telemed.professionalmanagement.domain.Specialty;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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
        SpecialtyEntity saved;
        try {
            saved = specialtyJpaRepository.save(entity);
        } catch (DataIntegrityViolationException exception) {
            if ("uq_specialties_name".equals(findConstraintName(exception))) {
                throw new DuplicateSpecialtyNameException(specialty.getName());
            }
            throw exception;
        }
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
