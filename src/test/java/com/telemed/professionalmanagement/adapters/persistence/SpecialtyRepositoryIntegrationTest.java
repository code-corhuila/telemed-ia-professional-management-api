package com.telemed.professionalmanagement.adapters.persistence;

import com.telemed.professionalmanagement.adapters.persistence.entity.SpecialtyEntity;
import com.telemed.professionalmanagement.adapters.persistence.repository.SpecialtyJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
class SpecialtyRepositoryIntegrationTest {

    @Autowired
    private SpecialtyJpaRepository specialtyJpaRepository;

    @Test
    void shouldPersistSpecialtyCorrectly() {
        SpecialtyEntity specialty = specialtyJpaRepository.saveAndFlush(new SpecialtyEntity(null, "Cardiology", "Heart care"));

        SpecialtyEntity found = specialtyJpaRepository.findById(specialty.getId()).orElseThrow();
        assertEquals("Cardiology", found.getName());
        assertEquals("Heart care", found.getDescription());
    }

    @Test
    void shouldRejectDuplicateSpecialtyName() {
        specialtyJpaRepository.saveAndFlush(new SpecialtyEntity(null, "Cardiology", "Heart care"));

        assertThrows(DataIntegrityViolationException.class, () -> specialtyJpaRepository.saveAndFlush(
                new SpecialtyEntity(null, "Cardiology", "Updated description")));
    }
}
