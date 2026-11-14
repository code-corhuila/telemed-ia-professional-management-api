package co.edu.corhuila.telemedia.professionalmanagement.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.AccessDeniedException;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.Caller;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.IdempotencyKeyReusedException;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.ProfessionalProfileCommand;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.Registration;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.ProfessionalRegistrationStore;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.StoredRegistration;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.DuplicateProfessionalIdentityException;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.DuplicateProfessionalLicenseException;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.InvalidBusinessDataException;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.Professional;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.ProfessionalStatus;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.ProfessionalType;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** DEC-015: step 2 of the onboarding saga creates the profile of an existing account. */
@ExtendWith(MockitoExtension.class)
class ProfessionalProfileServiceTest {

    private static final Caller WORKFLOW = new Caller("workflow", Set.of(Caller.SERVICE));
    private static final String KEY = "saga-1:create-profile";
    private static final UUID ACCOUNT = UUID.fromString("c1000000-0000-4000-8000-000000000071");
    private static final ProfessionalProfileCommand COMMAND = new ProfessionalProfileCommand(ACCOUNT, " RM-7100 ", 4);
    private static final Professional CREATED = new Professional(
            UUID.fromString("c0000000-0000-4000-8000-000000000071"), ACCOUNT, "RM-7100", 4,
            ProfessionalType.GENERAL_PRACTITIONER, ProfessionalStatus.ACTIVE);

    @Mock
    private ProfessionalRegistrationStore store;

    private ProfessionalProfileService service;

    @BeforeEach
    void setUp() {
        service = new ProfessionalProfileService(store, Set.of("workflow"));
    }

    @Test
    void theWorkflowCreatesAnActiveGeneralPractitionerForTheAccount() {
        when(store.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(store.saveRegistration(any(Professional.class), eq(KEY), anyString())).thenReturn(Optional.of(CREATED));

        Registration result = service.create(WORKFLOW, KEY, COMMAND);

        ArgumentCaptor<Professional> saved = ArgumentCaptor.forClass(Professional.class);
        verify(store).saveRegistration(saved.capture(), eq(KEY), anyString());
        assertThat(saved.getValue().getIdentityUserId()).isEqualTo(ACCOUNT);
        assertThat(saved.getValue().getLicenseNumber()).isEqualTo("RM-7100");
        assertThat(saved.getValue().getStatus()).isEqualTo(ProfessionalStatus.ACTIVE);
        assertThat(saved.getValue().getProfessionalType()).isEqualTo(ProfessionalType.GENERAL_PRACTITIONER);
        assertThat(result.created()).isTrue();
        assertThat(result.professional()).isSameAs(CREATED);
    }

    @Test
    void theSameKeyAndBodyReturnTheProfileAlreadyCreated() {
        when(store.findByIdempotencyKey(KEY))
                .thenReturn(Optional.of(new StoredRegistration(CREATED, ProfessionalProfileService.requestHash(COMMAND))));

        Registration result = service.create(WORKFLOW, KEY, COMMAND);

        assertThat(result.created()).isFalse();
        assertThat(result.professional()).isSameAs(CREATED);
        verify(store, never()).saveRegistration(any(), anyString(), anyString());
    }

    @Test
    void theSameKeyWithAnotherBodyIsRejected() {
        when(store.findByIdempotencyKey(KEY))
                .thenReturn(Optional.of(new StoredRegistration(CREATED, "hash-of-another-request")));

        assertThatThrownBy(() -> service.create(WORKFLOW, KEY, COMMAND))
                .isInstanceOf(IdempotencyKeyReusedException.class);
    }

    @Test
    void theHashIgnoresSurroundingSpacesOfTheLicense() {
        assertThat(ProfessionalProfileService.requestHash(new ProfessionalProfileCommand(ACCOUNT, "RM-7100", 4)))
                .isEqualTo(ProfessionalProfileService.requestHash(COMMAND));
    }

    @Test
    void aLicenseAlreadyRegisteredIsAConflict() {
        when(store.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(store.existsByLicenseNumber("RM-7100")).thenReturn(true);

        assertThatThrownBy(() -> service.create(WORKFLOW, KEY, COMMAND))
                .isInstanceOf(DuplicateProfessionalLicenseException.class);
        verify(store, never()).saveRegistration(any(), anyString(), anyString());
    }

    @Test
    void anAccountThatAlreadyHasAProfileIsAConflict() {
        when(store.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(store.saveRegistration(any(Professional.class), eq(KEY), anyString()))
                .thenThrow(new DuplicateProfessionalIdentityException(ACCOUNT));

        assertThatThrownBy(() -> service.create(WORKFLOW, KEY, COMMAND))
                .isInstanceOf(DuplicateProfessionalIdentityException.class);
    }

    @Test
    void aConcurrentTwinWithTheSameKeyIsAnsweredAsARetry() {
        StoredRegistration twin = new StoredRegistration(CREATED, ProfessionalProfileService.requestHash(COMMAND));
        when(store.findByIdempotencyKey(KEY)).thenReturn(Optional.empty(), Optional.of(twin));
        when(store.saveRegistration(any(Professional.class), eq(KEY), anyString())).thenReturn(Optional.empty());

        Registration result = service.create(WORKFLOW, KEY, COMMAND);

        assertThat(result.created()).isFalse();
        assertThat(result.professional()).isSameAs(CREATED);
    }

    @Test
    void theAccountIsRequired() {
        assertThatThrownBy(() -> service.create(WORKFLOW, KEY, new ProfessionalProfileCommand(null, "RM-7100", 4)))
                .isInstanceOf(InvalidBusinessDataException.class)
                .hasMessageContaining("accountId");
        verifyNoInteractions(store);
    }

    @Test
    void anAdministratorCannotCreateAProfileThisWay() {
        Caller admin = new Caller("admin-1", Set.of(Caller.ADMIN));

        assertThatThrownBy(() -> service.create(admin, KEY, COMMAND)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(store);
    }

    @Test
    void aServiceThatIsNotAWorkflowClientIsDenied() {
        Caller notifications = new Caller("notifications", Set.of(Caller.SERVICE));

        assertThatThrownBy(() -> service.create(notifications, KEY, COMMAND)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(store);
    }

    @Test
    void aUserNamedLikeTheWorkflowWithoutTheServiceRoleIsDenied() {
        Caller impostor = new Caller("workflow", Set.of("PATIENT"));

        assertThatThrownBy(() -> service.create(impostor, KEY, COMMAND)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(store);
    }
}
