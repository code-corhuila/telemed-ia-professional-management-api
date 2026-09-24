package com.telemed.professionalmanagement.application;

import com.telemed.professionalmanagement.application.command.RegisterProfessionalCommand;
import com.telemed.professionalmanagement.application.port.out.ProfessionalRepositoryPort;
import com.telemed.professionalmanagement.application.port.out.SpecialtyRepositoryPort;
import com.telemed.professionalmanagement.application.service.ProfessionalApplicationService;
import com.telemed.professionalmanagement.domain.InvalidBusinessDataException;
import com.telemed.professionalmanagement.domain.Professional;
import com.telemed.professionalmanagement.domain.ProfessionalNotFoundException;
import com.telemed.professionalmanagement.domain.Specialty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfessionalApplicationServiceTest {

    @Mock
    private ProfessionalRepositoryPort professionalRepositoryPort;

    @Mock
    private SpecialtyRepositoryPort specialtyRepositoryPort;

    @InjectMocks
    private ProfessionalApplicationService professionalApplicationService;

    @Test
    void shouldRegisterProfessionalSuccessfully() {
        Specialty specialty = new Specialty(9L, "Cardiology", "Heart health");
        when(specialtyRepositoryPort.findById(9L)).thenReturn(Optional.of(specialty));
        when(professionalRepositoryPort.existsByIdentityUserId(100L)).thenReturn(false);
        when(professionalRepositoryPort.existsByLicenseNumber("ABC-123")).thenReturn(false);
        when(professionalRepositoryPort.save(any(Professional.class)))
                .thenAnswer(invocation -> new Professional(7L, 100L, "ABC-123", 9L, 12));

        Professional professional = professionalApplicationService.register(
                new RegisterProfessionalCommand(100L, "ABC-123", 9L, 12)
        );

        assertEquals(7L, professional.getId());
        assertEquals(100L, professional.getIdentityUserId());
        assertEquals("ABC-123", professional.getLicenseNumber());
        assertEquals(9L, professional.getSpecialtyId());
        assertEquals(12, professional.getYearsExperience());
    }

    @Test
    void shouldAcceptLicenseNumberUpTo80Characters() {
        String licenseNumber = "A".repeat(80);

        Professional professional = new Professional(1L, 100L, licenseNumber, 9L, 5);

        assertEquals(80, professional.getLicenseNumber().length());
    }

    @Test
    void shouldRejectLicenseNumberLongerThan80Characters() {
        String licenseNumber = "A".repeat(81);

        assertThrows(InvalidBusinessDataException.class,
                () -> new Professional(1L, 100L, licenseNumber, 9L, 5));
    }

    @Test
    void shouldRejectInvalidYearsExperience() {
        assertThrows(InvalidBusinessDataException.class,
                () -> new Professional(1L, 100L, "ABC-123", 9L, -1));
    }

    @Test
    void shouldReturnProfessionalNotFoundWhenMissing() {
        when(professionalRepositoryPort.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ProfessionalNotFoundException.class, () -> professionalApplicationService.getById(99L));
    }

    @Test
    void shouldListProfessionalsSuccessfully() {
        when(professionalRepositoryPort.findAll()).thenReturn(List.of(
                new Professional(1L, 100L, "ABC-123", 9L, 8),
                new Professional(2L, 101L, "DEF-456", 10L, 10)
        ));

        List<Professional> professionals = professionalApplicationService.listAll();
        assertEquals(2, professionals.size());
    }
}
