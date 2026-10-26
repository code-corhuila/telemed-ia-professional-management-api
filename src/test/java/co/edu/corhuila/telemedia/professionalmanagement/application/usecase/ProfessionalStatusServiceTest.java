package co.edu.corhuila.telemedia.professionalmanagement.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.AccessDeniedException;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.Caller;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.ProfessionalRepositoryPort;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.InvalidBusinessDataException;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.Professional;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.ProfessionalNotFoundException;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.ProfessionalStatus;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.ProfessionalType;

@ExtendWith(MockitoExtension.class)
class ProfessionalStatusServiceTest {

    private static final Caller ADMIN = new Caller("admin-1", Set.of(Caller.ADMIN));
    private static final UUID ID = UUID.fromString("c0000000-0000-4000-8000-000000000041");
    private static final Professional ACTIVE = new Professional(ID,
            UUID.fromString("c1000000-0000-4000-8000-000000000041"), "RM-4100", 9,
            ProfessionalType.GENERAL_PRACTITIONER, ProfessionalStatus.ACTIVE);

    @Mock
    private ProfessionalRepositoryPort professionals;

    @InjectMocks
    private ProfessionalStatusService service;

    @Test
    void anAdministratorDeactivatesAProfileAndKeepsTheRestOfIt() {
        when(professionals.findById(ID)).thenReturn(Optional.of(ACTIVE));
        when(professionals.save(any(Professional.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Professional changed = service.changeStatus(ADMIN, ID, ProfessionalStatus.INACTIVE);

        ArgumentCaptor<Professional> saved = ArgumentCaptor.forClass(Professional.class);
        verify(professionals).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo(ID);
        assertThat(saved.getValue().getStatus()).isEqualTo(ProfessionalStatus.INACTIVE);
        assertThat(saved.getValue().getLicenseNumber()).isEqualTo("RM-4100");
        assertThat(saved.getValue().getIdentityUserId()).isEqualTo(ACTIVE.getIdentityUserId());
        assertThat(saved.getValue().getYearsExperience()).isEqualTo(9);
        assertThat(changed.getStatus()).isEqualTo(ProfessionalStatus.INACTIVE);
    }

    @Test
    void theStatusItAlreadyHasChangesNothing() {
        when(professionals.findById(ID)).thenReturn(Optional.of(ACTIVE));

        Professional unchanged = service.changeStatus(ADMIN, ID, ProfessionalStatus.ACTIVE);

        assertThat(unchanged).isSameAs(ACTIVE);
        verify(professionals, never()).save(any(Professional.class));
    }

    @Test
    void onlyAnAdministratorChangesTheStatus() {
        Caller professional = new Caller("professional-1", Set.of("PROFESSIONAL"));

        assertThatThrownBy(() -> service.changeStatus(professional, ID, ProfessionalStatus.INACTIVE))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(professionals);
    }

    @Test
    void anUnknownProfessionalIsNotFound() {
        when(professionals.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus(ADMIN, ID, ProfessionalStatus.INACTIVE))
                .isInstanceOf(ProfessionalNotFoundException.class);
    }

    @Test
    void aProfileNeedsAStatus() {
        assertThatThrownBy(() -> ACTIVE.withStatus(null)).isInstanceOf(InvalidBusinessDataException.class);
    }
}
