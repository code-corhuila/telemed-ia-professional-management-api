package com.telemed.professionalmanagement.application;

import com.telemed.professionalmanagement.application.command.CreateSpecialtyCommand;
import com.telemed.professionalmanagement.application.command.UpdateSpecialtyCommand;
import com.telemed.professionalmanagement.application.port.out.SpecialtyRepositoryPort;
import com.telemed.professionalmanagement.application.service.SpecialtyApplicationService;
import com.telemed.professionalmanagement.domain.Specialty;
import com.telemed.professionalmanagement.domain.SpecialtyNotFoundException;
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
class SpecialtyApplicationServiceTest {

    @Mock
    private SpecialtyRepositoryPort specialtyRepositoryPort;

    @InjectMocks
    private SpecialtyApplicationService specialtyApplicationService;

    @Test
    void shouldCreateSpecialtySuccessfully() {
        when(specialtyRepositoryPort.findByName("Cardiology")).thenReturn(Optional.empty());
        when(specialtyRepositoryPort.save(any(Specialty.class))).thenAnswer(invocation ->
                new Specialty(5L, "Cardiology", "Heart care"));

        Specialty created = specialtyApplicationService.create(new CreateSpecialtyCommand("Cardiology", "Heart care"));

        assertEquals(5L, created.getId());
        assertEquals("Cardiology", created.getName());
        assertEquals("Heart care", created.getDescription());
    }

    @Test
    void shouldUpdateSpecialtySuccessfully() {
        Specialty existing = new Specialty(3L, "Cardiology", "Old description");
        when(specialtyRepositoryPort.findById(3L)).thenReturn(Optional.of(existing));
        when(specialtyRepositoryPort.findByName("Neurology")).thenReturn(Optional.empty());
        when(specialtyRepositoryPort.save(any(Specialty.class))).thenAnswer(invocation ->
                new Specialty(3L, "Neurology", "Brain care"));

        Specialty updated = specialtyApplicationService.update(3L, new UpdateSpecialtyCommand("Neurology", "Brain care"));

        assertEquals("Neurology", updated.getName());
        assertEquals("Brain care", updated.getDescription());
    }

    @Test
    void shouldThrowWhenSpecialtyNotFound() {
        when(specialtyRepositoryPort.findById(99L)).thenReturn(Optional.empty());
        assertThrows(SpecialtyNotFoundException.class,
                () -> specialtyApplicationService.update(99L, new UpdateSpecialtyCommand("Any", "Any")));
    }

    @Test
    void shouldListSpecialtiesSuccessfully() {
        when(specialtyRepositoryPort.findAll()).thenReturn(List.of(
                new Specialty(1L, "Cardiology", "Heart"),
                new Specialty(2L, "Neurology", "Brain")
        ));

        List<Specialty> specialties = specialtyApplicationService.listAll();
        assertEquals(2, specialties.size());
    }
}
