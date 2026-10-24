package com.telemed.professionalmanagement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.telemed.professionalmanagement.application.command.ProfessionalRegistrationCommand;
import com.telemed.professionalmanagement.application.port.in.AccessDeniedException;
import com.telemed.professionalmanagement.application.port.in.Caller;
import com.telemed.professionalmanagement.application.port.in.IdempotencyKeyReusedException;
import com.telemed.professionalmanagement.application.port.in.Registration;
import com.telemed.professionalmanagement.application.port.out.IdentityAccountConflictException;
import com.telemed.professionalmanagement.application.port.out.IdentityAccountPort;
import com.telemed.professionalmanagement.application.port.out.IdentityUnavailableException;
import com.telemed.professionalmanagement.application.port.out.NewProfessionalAccount;
import com.telemed.professionalmanagement.application.port.out.ProfessionalRegistrationStore;
import com.telemed.professionalmanagement.application.port.out.StoredRegistration;
import com.telemed.professionalmanagement.application.service.ProfessionalRegistrationService;
import com.telemed.professionalmanagement.domain.DuplicateProfessionalLicenseException;
import com.telemed.professionalmanagement.domain.InvalidBusinessDataException;
import com.telemed.professionalmanagement.domain.Professional;
import com.telemed.professionalmanagement.domain.ProfessionalStatus;
import com.telemed.professionalmanagement.domain.ProfessionalType;

class ProfessionalRegistrationServiceTest {

    private static final Caller ADMIN = new Caller("admin-1", Set.of(Caller.ADMIN));
    private static final String KEY = "register-0001";

    private final FakeIdentity identity = new FakeIdentity();
    private final FakeStore store = new FakeStore();
    private final ProfessionalRegistrationService service = new ProfessionalRegistrationService(identity, store);

    private static ProfessionalRegistrationCommand command(String license) {
        return new ProfessionalRegistrationCommand("Ana Gómez", "ana@telemed.test", "1075000001",
                "temporary-pass-1", license, 6);
    }

    @Test
    void anAdministratorRegistersTheAccountAndAnActiveGeneralPractitioner() {
        Registration registration = service.register(ADMIN, KEY, command("RM-1001"));

        assertThat(registration.created()).isTrue();
        Professional professional = registration.professional();
        assertThat(professional.getId()).isNotNull();
        assertThat(professional.getIdentityUserId()).isEqualTo(identity.lastAccountId);
        assertThat(professional.getLicenseNumber()).isEqualTo("RM-1001");
        assertThat(professional.getYearsExperience()).isEqualTo(6);
        assertThat(professional.getProfessionalType()).isEqualTo(ProfessionalType.GENERAL_PRACTITIONER);
        assertThat(professional.getStatus()).isEqualTo(ProfessionalStatus.ACTIVE);
        assertThat(identity.calls).containsExactly(
                new NewProfessionalAccount("Ana Gómez", "ana@telemed.test", "1075000001", "temporary-pass-1"));
        assertThat(identity.keys).containsExactly(KEY);
    }

    @Test
    void onlyAnAdministratorRegisters() {
        Caller patient = new Caller("patient-1", Set.of("PATIENT"));

        assertThatThrownBy(() -> service.register(patient, KEY, command("RM-1001")))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(identity.calls).isEmpty();
        assertThat(store.rows).isEmpty();
    }

    @Test
    void aRetryWithTheSameKeyAndBodyReturnsTheSameProfessionalWithoutAnotherAccount() {
        Registration first = service.register(ADMIN, KEY, command("RM-1001"));

        Registration retry = service.register(ADMIN, KEY, command("RM-1001"));

        assertThat(retry.created()).isFalse();
        assertThat(retry.professional().getId()).isEqualTo(first.professional().getId());
        assertThat(identity.calls).hasSize(1);
    }

    @Test
    void theSameKeyWithAnotherBodyIsRejectedWithoutTouchingIdentity() {
        service.register(ADMIN, KEY, command("RM-1001"));

        assertThatThrownBy(() -> service.register(ADMIN, KEY, command("RM-2002")))
                .isInstanceOf(IdempotencyKeyReusedException.class);
        assertThat(identity.calls).hasSize(1);
    }

    @Test
    void aLicenseAlreadyRegisteredIsAConflictBeforeAnyAccountIsCreated() {
        service.register(ADMIN, KEY, command("RM-1001"));

        assertThatThrownBy(() -> service.register(ADMIN, "register-0002", command("RM-1001")))
                .isInstanceOf(DuplicateProfessionalLicenseException.class);
        assertThat(identity.calls).hasSize(1);
    }

    @Test
    void anInvalidProfileIsRejectedBeforeAnyAccountIsCreated() {
        ProfessionalRegistrationCommand negative = new ProfessionalRegistrationCommand("Ana Gómez",
                "ana@telemed.test", "1075000001", "temporary-pass-1", "RM-1001", -1);

        assertThatThrownBy(() -> service.register(ADMIN, KEY, negative))
                .isInstanceOf(InvalidBusinessDataException.class);
        assertThatThrownBy(() -> service.register(ADMIN, KEY, command("A".repeat(81))))
                .isInstanceOf(InvalidBusinessDataException.class);
        assertThat(identity.calls).isEmpty();
    }

    @Test
    void anAccountThatAlreadyExistsInIdentityIsAConflictAndNothingIsStored() {
        identity.failWith = new IdentityAccountConflictException();

        assertThatThrownBy(() -> service.register(ADMIN, KEY, command("RM-1001")))
                .isInstanceOf(IdentityAccountConflictException.class);
        assertThat(store.rows).isEmpty();
    }

    @Test
    void anUnavailableIdentityServiceStoresNothing() {
        identity.failWith = new IdentityUnavailableException("down");

        assertThatThrownBy(() -> service.register(ADMIN, KEY, command("RM-1001")))
                .isInstanceOf(IdentityUnavailableException.class);
        assertThat(store.rows).isEmpty();
    }

    @Test
    void aConcurrentTwinWithTheSameKeyIsAnsweredWithTheStoredProfessional() {
        Registration twin = service.register(ADMIN, KEY, command("RM-1001"));
        // The key was free when checked; the twin stored it before this request did.
        store.hideKeyOnce = KEY;

        Registration late = service.register(ADMIN, KEY, command("RM-1001"));

        assertThat(late.created()).isFalse();
        assertThat(late.professional().getId()).isEqualTo(twin.professional().getId());
    }

    @Test
    void thePasswordNeverAppearsInTheCommandText() {
        assertThat(command("RM-1001").toString()).doesNotContain("temporary-pass-1");
        assertThat(new NewProfessionalAccount("Ana", "ana@telemed.test", "1", "temporary-pass-1").toString())
                .doesNotContain("temporary-pass-1");
    }

    private static final class FakeIdentity implements IdentityAccountPort {
        final List<NewProfessionalAccount> calls = new ArrayList<>();
        final List<String> keys = new ArrayList<>();
        RuntimeException failWith;
        UUID lastAccountId;

        @Override
        public UUID createProfessionalAccount(NewProfessionalAccount account, String idempotencyKey) {
            if (failWith != null) {
                throw failWith;
            }
            calls.add(account);
            keys.add(idempotencyKey);
            lastAccountId = UUID.randomUUID();
            return lastAccountId;
        }
    }

    private static final class FakeStore implements ProfessionalRegistrationStore {
        final Map<String, StoredRegistration> rows = new HashMap<>();
        String hideKeyOnce;

        @Override
        public Optional<StoredRegistration> findByIdempotencyKey(String key) {
            if (key.equals(hideKeyOnce)) {
                hideKeyOnce = null;
                return Optional.empty();
            }
            return Optional.ofNullable(rows.get(key));
        }

        @Override
        public boolean existsByLicenseNumber(String licenseNumber) {
            return rows.values().stream()
                    .anyMatch(row -> row.professional().getLicenseNumber().equals(licenseNumber.trim()));
        }

        @Override
        public Optional<Professional> saveRegistration(Professional professional, String key, String requestHash) {
            if (rows.containsKey(key)) {
                return Optional.empty();
            }
            Professional stored = new Professional(UUID.randomUUID(), professional.getIdentityUserId(),
                    professional.getLicenseNumber(), professional.getYearsExperience(),
                    professional.getProfessionalType(), professional.getStatus());
            rows.put(key, new StoredRegistration(stored, requestHash));
            return Optional.of(stored);
        }
    }
}
