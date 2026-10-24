package com.telemed.professionalmanagement.adapters.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.telemed.professionalmanagement.adapters.persistence.entity.ProfessionalEntity;
import com.telemed.professionalmanagement.adapters.persistence.repository.ProfessionalJpaRepository;

@DataJpaTest
@ActiveProfiles("test")
class ProfessionalRepositoryIntegrationTest {

    @Autowired
    private ProfessionalJpaRepository professionalJpaRepository;

    private static ProfessionalEntity professional(Long identityUserId, String licenseNumber, int yearsExperience) {
        return new ProfessionalEntity(null, identityUserId, licenseNumber, yearsExperience,
                "GENERAL_PRACTITIONER", "ACTIVE");
    }

    @Test
    void shouldMapTheProfessionalProfile() {
        ProfessionalEntity saved = professionalJpaRepository.saveAndFlush(professional(100L, "ABC-123", 8));

        ProfessionalEntity found = professionalJpaRepository.findById(saved.getId()).orElseThrow();
        assertEquals(100L, found.getIdentityUserId());
        assertEquals("ABC-123", found.getLicenseNumber());
        assertEquals(8, found.getYearsExperience());
        assertEquals("GENERAL_PRACTITIONER", found.getProfessionalType());
        assertEquals("ACTIVE", found.getStatus());
    }

    @Test
    void shouldRejectDuplicateIdentityUserId() {
        professionalJpaRepository.saveAndFlush(professional(100L, "ABC-123", 5));

        assertThrows(DataIntegrityViolationException.class,
                () -> professionalJpaRepository.saveAndFlush(professional(100L, "XYZ-999", 7)));
    }

    @Test
    void shouldRejectDuplicateLicenseNumber() {
        professionalJpaRepository.saveAndFlush(professional(100L, "ABC-123", 5));

        assertThrows(DataIntegrityViolationException.class,
                () -> professionalJpaRepository.saveAndFlush(professional(200L, "ABC-123", 7)));
    }
}
