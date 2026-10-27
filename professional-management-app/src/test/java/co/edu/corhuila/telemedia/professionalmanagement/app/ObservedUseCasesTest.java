package co.edu.corhuila.telemedia.professionalmanagement.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.AccessDeniedException;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.Caller;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.ChangeProfessionalStatusUseCase;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.ProfessionalRegistrationCommand;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.RegisterProfessionalAccountUseCase;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.Registration;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.IdentityUnavailableException;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.DuplicateProfessionalLicenseException;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.Professional;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.ProfessionalNotFoundException;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.ProfessionalStatus;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.ProfessionalType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * NFR-005: the registration and the status change leave one domain event in the log and count
 * their outcome, without personal data in either.
 */
class ObservedUseCasesTest {

    private static final Caller ADMIN = new Caller("admin-1", Set.of(Caller.ADMIN));
    private static final ProfessionalRegistrationCommand COMMAND = new ProfessionalRegistrationCommand(
            "Laura Gómez", "laura@example.test", "1020304050", "Temporal-2026", "RM-0001", 4);

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final Professional professional = new Professional(UUID.randomUUID(), UUID.randomUUID(), "RM-0001", 4,
            ProfessionalType.GENERAL_PRACTITIONER, ProfessionalStatus.ACTIVE);

    @BeforeEach
    void captureLogs() {
        logs.start();
        for (Class<?> type : List.of(ObservedRegistration.class, ObservedStatusChange.class)) {
            ((Logger) LoggerFactory.getLogger(type)).addAppender(logs);
        }
    }

    @AfterEach
    void releaseLogs() {
        for (Class<?> type : List.of(ObservedRegistration.class, ObservedStatusChange.class)) {
            ((Logger) LoggerFactory.getLogger(type)).detachAppender(logs);
        }
    }

    private ObservedRegistration registration(RegisterProfessionalAccountUseCase delegate) {
        return new ObservedRegistration(delegate, meters);
    }

    private double registrations(String outcome) {
        var counter = meters.find("professional.registrations").tag("outcome", outcome).counter();
        return counter == null ? 0 : counter.count();
    }

    private List<Map<String, Object>> events() {
        return logs.list.stream().filter(line -> line.getKeyValuePairs() != null).map(line -> {
            Map<String, Object> fields = new HashMap<>();
            line.getKeyValuePairs().forEach(pair -> fields.put(pair.key, pair.value));
            fields.put("level", line.getLevel().toString());
            return fields;
        }).toList();
    }

    @Test
    void aNewRegistrationIsLoggedAndCountedAsCreated() {
        Registration result = registration((caller, key, command) -> new Registration(professional, true))
                .register(ADMIN, "key-0001-abcd", COMMAND);

        assertThat(result.created()).isTrue();
        assertThat(registrations("created")).isEqualTo(1);
        assertThat(events()).singleElement().satisfies(event -> {
            assertThat(event).containsEntry("event", "professional.registration.created")
                    .containsEntry("professional_id", professional.getId().toString())
                    .containsEntry("level", "INFO").containsKey("duration_ms");
        });
    }

    @Test
    void aRepeatedKeyIsCountedAsReplayed() {
        registration((caller, key, command) -> new Registration(professional, false))
                .register(ADMIN, "key-0001-abcd", COMMAND);

        assertThat(registrations("replayed")).isEqualTo(1);
        assertThat(events()).singleElement().satisfies(event ->
                assertThat(event).containsEntry("event", "professional.registration.replayed"));
    }

    @Test
    void aRejectedRegistrationKeepsItsErrorAndIsCountedAsRejected() {
        var registration = registration((caller, key, command) -> {
            throw new DuplicateProfessionalLicenseException("RM-0001");
        });

        assertThatThrownBy(() -> registration.register(ADMIN, "key-0001-abcd", COMMAND))
                .isInstanceOf(DuplicateProfessionalLicenseException.class);
        assertThat(registrations("rejected")).isEqualTo(1);
        assertThat(events()).singleElement().satisfies(event -> assertThat(event)
                .containsEntry("event", "professional.registration.rejected")
                .containsEntry("reason", "DuplicateProfessionalLicenseException"));
    }

    @Test
    void anUnavailableIdentityIsCountedApartAndLoggedAsAWarning() {
        var registration = registration((caller, key, command) -> {
            throw new IdentityUnavailableException("identity answered 502");
        });

        assertThatThrownBy(() -> registration.register(ADMIN, "key-0001-abcd", COMMAND))
                .isInstanceOf(IdentityUnavailableException.class);
        assertThat(registrations("unavailable")).isEqualTo(1);
        assertThat(events()).singleElement().satisfies(event -> assertThat(event)
                .containsEntry("event", "professional.registration.unavailable").containsEntry("level", "WARN"));
    }

    @Test
    void anUnexpectedErrorIsCountedAsFailed() {
        var registration = registration((caller, key, command) -> {
            throw new IllegalStateException("broken");
        });

        assertThatThrownBy(() -> registration.register(ADMIN, "key-0001-abcd", COMMAND))
                .isInstanceOf(IllegalStateException.class);
        assertThat(registrations("failed")).isEqualTo(1);
        assertThat(events()).singleElement().satisfies(event -> assertThat(event)
                .containsEntry("event", "professional.registration.failed").containsEntry("level", "ERROR"));
    }

    @Test
    void theLogNeverCarriesThePersonalDataOfTheRequest() {
        registration((caller, key, command) -> new Registration(professional, true))
                .register(ADMIN, "key-0001-abcd", COMMAND);

        String written = logs.list.toString() + events();
        assertThat(written).doesNotContain("Laura", "laura@example.test", "1020304050", "Temporal-2026",
                "key-0001-abcd");
    }

    @Test
    void aStatusChangeIsLoggedWithTheNewStatusAndCounted() {
        Professional inactive = professional.withStatus(ProfessionalStatus.INACTIVE);
        ChangeProfessionalStatusUseCase delegate = (caller, id, status) -> inactive;

        new ObservedStatusChange(delegate, meters).changeStatus(ADMIN, professional.getId(), ProfessionalStatus.INACTIVE);

        assertThat(meters.find("professional.status.changes").tag("outcome", "applied").counter().count()).isEqualTo(1);
        assertThat(events()).singleElement().satisfies(event -> assertThat(event)
                .containsEntry("event", "professional.status.changed")
                .containsEntry("professional_id", professional.getId().toString())
                .containsEntry("status", "INACTIVE"));
    }

    @Test
    void aRejectedStatusChangeIsCountedAsRejected() {
        ChangeProfessionalStatusUseCase notFound = (caller, id, status) -> {
            throw new ProfessionalNotFoundException(id);
        };
        ChangeProfessionalStatusUseCase denied = (caller, id, status) -> {
            throw new AccessDeniedException();
        };

        for (ChangeProfessionalStatusUseCase delegate : List.of(notFound, denied)) {
            assertThatThrownBy(() -> new ObservedStatusChange(delegate, meters)
                    .changeStatus(ADMIN, UUID.randomUUID(), ProfessionalStatus.ACTIVE)).isInstanceOf(RuntimeException.class);
        }

        assertThat(meters.find("professional.status.changes").tag("outcome", "rejected").counter().count()).isEqualTo(2);
        assertThat(events()).extracting(event -> event.get("event")).containsOnly("professional.status.rejected");
    }
}
