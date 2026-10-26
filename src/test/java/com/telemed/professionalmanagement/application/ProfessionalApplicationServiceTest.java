package com.telemed.professionalmanagement.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.telemed.professionalmanagement.application.port.in.AccessDeniedException;
import com.telemed.professionalmanagement.application.port.in.Caller;
import com.telemed.professionalmanagement.application.port.in.Page;
import com.telemed.professionalmanagement.application.port.in.PageRequest;
import com.telemed.professionalmanagement.application.port.in.ProfessionalView;
import com.telemed.professionalmanagement.application.port.out.AccountDirectory;
import com.telemed.professionalmanagement.application.port.out.AccountName;
import com.telemed.professionalmanagement.application.port.out.IdentityUnavailableException;
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
    private static final Caller PATIENT = new Caller("patient-1", Set.of("PATIENT"));
    private static final Caller PROFESSIONAL = new Caller("professional-1", Set.of("PROFESSIONAL"));

    private static final UUID ID = UUID.fromString("c0000000-0000-4000-8000-000000000007");
    private static final UUID ACCOUNT = UUID.fromString("c1000000-0000-4000-8000-000000000100");
    private static final UUID OTHER_ACCOUNT = UUID.fromString("c1000000-0000-4000-8000-000000000101");

    @Mock
    private ProfessionalRepositoryPort professionalRepositoryPort;

    @Mock
    private AccountDirectory accountDirectory;

    @InjectMocks
    private ProfessionalApplicationService professionalApplicationService;

    private static Professional stored(UUID id, UUID identityUserId, String licenseNumber, int yearsExperience) {
        return new Professional(id, identityUserId, licenseNumber, yearsExperience,
                ProfessionalType.GENERAL_PRACTITIONER, ProfessionalStatus.ACTIVE);
    }

    @Test
    void shouldAcceptLicenseNumberUpTo80Characters() {
        Professional professional = Professional.register(ACCOUNT, "A".repeat(80), 5);

        assertEquals(80, professional.getLicenseNumber().length());
    }

    @Test
    void shouldRejectLicenseNumberLongerThan80Characters() {
        assertThrows(InvalidBusinessDataException.class, () -> Professional.register(ACCOUNT, "A".repeat(81), 5));
    }

    @Test
    void shouldRejectInvalidYearsExperience() {
        assertThrows(InvalidBusinessDataException.class, () -> Professional.register(ACCOUNT, "ABC-123", -1));
    }

    @Test
    void aStoredProfessionalNeedsItsTypeAndStatus() {
        assertThrows(InvalidBusinessDataException.class,
                () -> new Professional(ID, ACCOUNT, "ABC-123", 5, null, ProfessionalStatus.ACTIVE));
        assertThrows(InvalidBusinessDataException.class,
                () -> new Professional(ID, ACCOUNT, "ABC-123", 5, ProfessionalType.GENERAL_PRACTITIONER, null));
    }

    @Test
    void shouldReturnProfessionalNotFoundWhenMissing() {
        when(professionalRepositoryPort.findById(ID)).thenReturn(Optional.empty());
        assertThrows(ProfessionalNotFoundException.class, () -> professionalApplicationService.getById(ADMIN, ID));
    }

    @Test
    void theListReturnsTheRequestedPageOfTheRepository() {
        PageRequest request = new PageRequest(2, 2);
        Page<Professional> stored = Page.of(List.of(
                stored(UUID.randomUUID(), ACCOUNT, "ABC-123", 8),
                stored(UUID.randomUUID(), OTHER_ACCOUNT, "DEF-456", 10)), request, 5);
        when(professionalRepositoryPort.findPage(request)).thenReturn(stored);

        Page<ProfessionalView> page = professionalApplicationService.list(ADMIN, request);

        assertEquals(2, page.items().size());
        assertEquals(2, page.page());
        assertEquals(5, page.total());
        assertEquals(3, page.totalPages());
    }

    @Test
    void theListCarriesTheAccountNamesOfThePageAskedInOneCall() {
        PageRequest request = new PageRequest(1, 20);
        Professional ana = stored(UUID.randomUUID(), ACCOUNT, "ABC-123", 8);
        Professional luis = stored(UUID.randomUUID(), OTHER_ACCOUNT, "DEF-456", 10);
        when(professionalRepositoryPort.findPage(request)).thenReturn(Page.of(List.of(ana, luis), request, 2));
        when(accountDirectory.namesOf(List.of(ACCOUNT, OTHER_ACCOUNT)))
                .thenReturn(Map.of(ACCOUNT, new AccountName("Ana Gómez", "ana@telemed.test")));

        Page<ProfessionalView> page = professionalApplicationService.list(ADMIN, request);

        assertEquals(new ProfessionalView(ana, new AccountName("Ana Gómez", "ana@telemed.test")), page.items().get(0));
        assertEquals(new ProfessionalView(luis, null), page.items().get(1));
        verify(accountDirectory).namesOf(List.of(ACCOUNT, OTHER_ACCOUNT));
    }

    @Test
    void theListIsReturnedWithoutNamesWhenIdentityDoesNotAnswer() {
        PageRequest request = new PageRequest(1, 20);
        Professional ana = stored(UUID.randomUUID(), ACCOUNT, "ABC-123", 8);
        when(professionalRepositoryPort.findPage(request)).thenReturn(Page.of(List.of(ana), request, 1));
        when(accountDirectory.namesOf(any())).thenThrow(new IdentityUnavailableException("down"));

        Page<ProfessionalView> page = professionalApplicationService.list(ADMIN, request);

        assertEquals(List.of(new ProfessionalView(ana, null)), page.items());
    }

    @Test
    void anEmptyPageDoesNotAskForNames() {
        PageRequest request = new PageRequest(1, 20);
        when(professionalRepositoryPort.findPage(request)).thenReturn(Page.of(List.of(), request, 0));

        assertEquals(0, professionalApplicationService.list(ADMIN, request).items().size());
        verifyNoInteractions(accountDirectory);
    }

    @Test
    void theDetailCarriesTheAccountNameOrNoneWhenIdentityDoesNotAnswer() {
        Professional ana = stored(ID, ACCOUNT, "ABC-123", 8);
        when(professionalRepositoryPort.findById(ID)).thenReturn(Optional.of(ana));
        when(accountDirectory.namesOf(List.of(ACCOUNT)))
                .thenReturn(Map.of(ACCOUNT, new AccountName("Ana Gómez", "ana@telemed.test")))
                .thenThrow(new IdentityUnavailableException("down"));

        assertEquals(new ProfessionalView(ana, new AccountName("Ana Gómez", "ana@telemed.test")),
                professionalApplicationService.getById(ADMIN, ID));
        assertEquals(new ProfessionalView(ana, null), professionalApplicationService.getById(ADMIN, ID));
    }

    @Test
    void onlyAnAdministratorReadsTheAccountEmailInTheList() {
        // DEC-009: the e-mail is personal data the other roles do not need; the name stays.
        PageRequest request = new PageRequest(1, 20);
        Professional ana = stored(UUID.randomUUID(), ACCOUNT, "ABC-123", 8);
        when(professionalRepositoryPort.findPage(request)).thenReturn(Page.of(List.of(ana), request, 1));
        when(accountDirectory.namesOf(List.of(ACCOUNT)))
                .thenReturn(Map.of(ACCOUNT, new AccountName("Ana Gómez", "ana@telemed.test")));

        assertEquals(List.of(new ProfessionalView(ana, new AccountName("Ana Gómez", null))),
                professionalApplicationService.list(PATIENT, request).items());
        assertEquals(List.of(new ProfessionalView(ana, new AccountName("Ana Gómez", null))),
                professionalApplicationService.list(PROFESSIONAL, request).items());
        assertEquals(List.of(new ProfessionalView(ana, new AccountName("Ana Gómez", "ana@telemed.test"))),
                professionalApplicationService.list(ADMIN, request).items());
    }

    @Test
    void onlyAnAdministratorReadsTheAccountEmailInTheDetail() {
        Professional ana = stored(ID, ACCOUNT, "ABC-123", 8);
        when(professionalRepositoryPort.findById(ID)).thenReturn(Optional.of(ana));
        when(accountDirectory.namesOf(List.of(ACCOUNT)))
                .thenReturn(Map.of(ACCOUNT, new AccountName("Ana Gómez", "ana@telemed.test")));

        assertEquals(new ProfessionalView(ana, new AccountName("Ana Gómez", null)),
                professionalApplicationService.getById(PATIENT, ID));
        assertEquals(new ProfessionalView(ana, new AccountName("Ana Gómez", null)),
                professionalApplicationService.getById(PROFESSIONAL, ID));
        assertEquals(new ProfessionalView(ana, new AccountName("Ana Gómez", "ana@telemed.test")),
                professionalApplicationService.getById(ADMIN, ID));
    }

}
