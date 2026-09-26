package com.telemed.professionalmanagement.adapters.persistence;

import com.telemed.professionalmanagement.adapters.persistence.entity.ProfessionalEntity;
import com.telemed.professionalmanagement.adapters.persistence.entity.SpecialtyEntity;
import com.telemed.professionalmanagement.adapters.persistence.repository.ProfessionalJpaRepository;
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
class ProfessionalRepositoryIntegrationTest {

    @Autowired
    private ProfessionalJpaRepository professionalJpaRepository;

    @Autowired
    private SpecialtyJpaRepository specialtyJpaRepository;

    @Test
    void shouldMapProfessionalAndSpecialtyRelationship() {
        SpecialtyEntity specialty = specialtyJpaRepository.saveAndFlush(new SpecialtyEntity(null, "Cardiology", "Heart care"));
        ProfessionalEntity professional = professionalJpaRepository.saveAndFlush(
                new ProfessionalEntity(null, 100L, "ABC-123", specialty, 8)
        );

        ProfessionalEntity found = professionalJpaRepository.findById(professional.getId()).orElseThrow();
        assertEquals(100L, found.getIdentityUserId());
        assertEquals("ABC-123", found.getLicenseNumber());
        assertEquals("Cardiology", found.getSpecialty().getName());
        assertEquals(8, found.getYearsExperience());
    }

    @Test
    void shouldRejectDuplicateIdentityUserId() {
        SpecialtyEntity specialty = specialtyJpaRepository.saveAndFlush(new SpecialtyEntity(null, "Cardiology", "Heart care"));
        professionalJpaRepository.saveAndFlush(new ProfessionalEntity(null, 100L, "ABC-123", specialty, 5));

        assertThrows(DataIntegrityViolationException.class, () -> professionalJpaRepository.saveAndFlush(
                new ProfessionalEntity(null, 100L, "XYZ-999", specialty, 7)));
    }

    @Test
    void shouldRejectDuplicateLicenseNumber() {
        SpecialtyEntity specialty = specialtyJpaRepository.saveAndFlush(new SpecialtyEntity(null, "Neurology", "Brain"));
        professionalJpaRepository.saveAndFlush(new ProfessionalEntity(null, 100L, "ABC-123", specialty, 5));

        assertThrows(DataIntegrityViolationException.class, () -> professionalJpaRepository.saveAndFlush(
                new ProfessionalEntity(null, 200L, "ABC-123", specialty, 7)));
    }
}
