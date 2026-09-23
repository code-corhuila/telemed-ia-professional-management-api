package com.telemed.professionalmanagement.adapters.persistence.repository;

import com.telemed.professionalmanagement.adapters.persistence.entity.SpecialtyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpecialtyJpaRepository extends JpaRepository<SpecialtyEntity, Long> {
    Optional<SpecialtyEntity> findByName(String name);
}
