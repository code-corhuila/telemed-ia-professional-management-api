package com.telemed.professionalmanagement.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.telemed.professionalmanagement.application.command.RegisterProfessionalCommand;
import com.telemed.professionalmanagement.application.port.in.AccessDeniedException;
import com.telemed.professionalmanagement.application.port.in.Caller;
import com.telemed.professionalmanagement.application.port.out.ProfessionalRepositoryPort;
import com.telemed.professionalmanagement.application.service.ProfessionalApplicationService;
import com.telemed.professionalmanagement.domain.DuplicateProfessionalIdentityException;
import com.telemed.professionalmanagement.domain.DuplicateProfessionalLicenseException;
import com.telemed.professionalmanagement.domain.InvalidBusinessDataException;
import com.telemed.professionalmanagement.domain.Professional;
import com.telemed.professionalmanagement.domain.ProfessionalNotFoundException;
import com.telemed.professionalmanagement.domain.ProfessionalStatus;
import com.telemed.professionalmanagement.domain.ProfessionalType;

@ExtendWith(MockitoExtension.class)
class ProfessionalApplicationServiceTest {

    private static final Caller ADMIN = new Caller("admin-1", Set.of(Caller.ADMIN));

    @Mock
    private ProfessionalRepositoryPort professionalRepositoryPort;

    @InjectMocks
    private ProfessionalApplicationService professionalApplicationService;

    private static Professional stored(Long id, Long identityUserId, String licenseNumber, int yearsExperience) {
        return new Professional(id, identityUserId, licenseNumber, yearsExperience,
                ProfessionalType.GENERAL_PRACTITIONER, ProfessionalStatus.ACTIVE);
    }

    @Test
    void aRegisteredProfessionalIsAnActiveGeneralPractitioner() {
        when(professionalRepositoryPort.existsByIdentityUserId(100L)).thenReturn(false);
        when(professionalRepositoryPort.existsByLicenseNumber("ABC-123")).thenReturn(false);
        when(professionalRepositoryPort.save(any(Professional.class))).thenAnswer(invocation -> {
            Professional candidate = invocation.getArgument(0);
            return new Professional(7L, candidate.getIdentityUserId(), candidate.getLicenseNumber(),
                    candidate.getYearsExperience(), candidate.getProfessionalType(), candidate.getStatus());
        });

        Professional professional = professionalApplicationService.register(ADMIN,
                new RegisterProfessionalCommand(100L, "ABC-123", 12));

        ArgumentCaptor<Professional> saved = ArgumentCaptor.forClass(Professional.class);
        verify(professionalRepositoryPort).save(saved.capture());
        assertNull(saved.getValue().getId());
        assertEquals(ProfessionalType.GENERAL_PRACTITIONER, saved.getValue().getProfessionalType());
        assertEquals(ProfessionalStatus.ACTIVE, saved.getValue().getStatus());
        assertEquals(7L, professional.getId());
        assertEquals(100L, professional.getIdentityUserId());
        assertEquals("ABC-123", professional.getLicenseNumber());
        assertEquals(12, professional.getYearsExperience());
    }

    @Test
    void aDuplicateAccountOrLicenseIsAConflict() {
        when(professionalRepositoryPort.existsByIdentityUserId(100L)).thenReturn(true);
        assertThrows(DuplicateProfessionalIdentityException.class, () -> professionalApplicationService.register(ADMIN,
                new RegisterProfessionalCommand(100L, "ABC-123", 12)));

        when(professionalRepositoryPort.existsByIdentityUserId(101L)).thenReturn(false);
        when(professionalRepositoryPort.existsByLicenseNumber("ABC-123")).thenReturn(true);
        assertThrows(DuplicateProfessionalLicenseException.class, () -> professionalApplicationService.register(ADMIN,
                new RegisterProfessionalCommand(101L, "ABC-123", 12)));
    }

    @Test
    void shouldAcceptLicenseNumberUpTo80Characters() {
        Professional professional = Professional.register(100L, "A".repeat(80), 5);

        assertEquals(80, professional.getLicenseNumber().length());
    }

    @Test
    void shouldRejectLicenseNumberLongerThan80Characters() {
        assertThrows(InvalidBusinessDataException.class, () -> Professional.register(100L, "A".repeat(81), 5));
    }

    @Test
    void shouldRejectInvalidYearsExperience() {
        assertThrows(InvalidBusinessDataException.class, () -> Professional.register(100L, "ABC-123", -1));
    }

    @Test
    void aStoredProfessionalNeedsItsTypeAndStatus() {
        assertThrows(InvalidBusinessDataException.class,
                () -> new Professional(1L, 100L, "ABC-123", 5, null, ProfessionalStatus.ACTIVE));
        assertThrows(InvalidBusinessDataException.class,
                () -> new Professional(1L, 100L, "ABC-123", 5, ProfessionalType.GENERAL_PRACTITIONER, null));
    }

    @Test
    void shouldReturnProfessionalNotFoundWhenMissing() {
        when(professionalRepositoryPort.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ProfessionalNotFoundException.class, () -> professionalApplicationService.getById(99L));
    }

    @Test
    void shouldListProfessionalsSuccessfully() {
        when(professionalRepositoryPort.findAll()).thenReturn(List.of(
                stored(1L, 100L, "ABC-123", 8),
                stored(2L, 101L, "DEF-456", 10)));

        assertEquals(2, professionalApplicationService.listAll().size());
    }

    @Test
    void onlyAnAdministratorRegistersAProfessional() {
        Caller patient = new Caller("patient-1", Set.of("PATIENT"));

        assertThrows(AccessDeniedException.class, () -> professionalApplicationService.register(patient,
                new RegisterProfessionalCommand(99L, "ABC-123", 12)));
        verifyNoInteractions(professionalRepositoryPort);
    }
}
