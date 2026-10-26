package com.telemed.professionalmanagement.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.telemed.professionalmanagement.adapters.persistence.adapter.ProfessionalRepositoryAdapter;
import com.telemed.professionalmanagement.adapters.persistence.mapper.ProfessionalPersistenceMapper;
import com.telemed.professionalmanagement.application.port.in.Page;
import com.telemed.professionalmanagement.application.port.in.PageRequest;
import com.telemed.professionalmanagement.domain.DuplicateProfessionalIdentityException;
import com.telemed.professionalmanagement.domain.DuplicateProfessionalLicenseException;
import com.telemed.professionalmanagement.domain.Professional;
import com.telemed.professionalmanagement.domain.ProfessionalStatus;
import com.telemed.professionalmanagement.domain.ProfessionalType;

/**
 * The persistence adapter against the real engine and the real schema, built by
 * {@code telemed-ia-professional-management-db} (numeral 5.3). Hibernate validates the
 * mapping against that schema. Each test runs in a transaction that is rolled back.
 * Without {@code TEST_DATABASE_URL} the class is skipped (Anexo I).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ProfessionalRepositoryAdapter.class, ProfessionalPersistenceMapper.class})
@EnabledIfEnvironmentVariable(named = "TEST_DATABASE_URL", matches = ".+")
class ProfessionalRepositoryPostgresTest {

    @DynamicPropertySource
    static void realDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("TEST_DATABASE_URL"));
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private ProfessionalRepositoryAdapter adapter;

    @Autowired
    private JdbcTemplate jdbc;

    private static String license() {
        return "IT-" + UUID.randomUUID();
    }

    @Test
    void aRegisteredProfessionalIsStoredInTheDomainTableWithAUuid() {
        UUID account = UUID.randomUUID();
        String license = license();

        Professional saved = adapter.save(Professional.register(account, license, 7));

        assertThat(saved.getId()).isNotNull();
        assertThat(jdbc.queryForMap("""
                SELECT identity_user_id, license_number, years_experience, professional_type, status
                FROM professional_management.professional WHERE id = ?""", saved.getId()))
                .containsEntry("identity_user_id", account)
                .containsEntry("license_number", license)
                .containsEntry("years_experience", 7)
                .containsEntry("professional_type", "GENERAL_PRACTITIONER")
                .containsEntry("status", "ACTIVE");
    }

    @Test
    void aStoredProfessionalIsReadBack() {
        UUID account = UUID.randomUUID();
        String license = license();
        Professional saved = adapter.save(Professional.register(account, license, 3));

        Professional found = adapter.findById(saved.getId()).orElseThrow();

        assertThat(found.getIdentityUserId()).isEqualTo(account);
        assertThat(found.getLicenseNumber()).isEqualTo(license);
        assertThat(found.getYearsExperience()).isEqualTo(3);
        assertThat(found.getProfessionalType()).isEqualTo(ProfessionalType.GENERAL_PRACTITIONER);
        assertThat(found.getStatus()).isEqualTo(ProfessionalStatus.ACTIVE);
        assertThat(adapter.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void aPageGoesFromTheNewestToTheOldestAndCountsTheWholeTable() {
        // DEC-010. Rows written in one test transaction share now(), so each gets its own
        // registration time, later than any other row of the table.
        String prefix = "IT-PAGE-" + UUID.randomUUID() + "-";
        registeredAt(adapter.save(Professional.register(UUID.randomUUID(), prefix + "old", 1)), "2999-01-01T00:00:00Z");
        registeredAt(adapter.save(Professional.register(UUID.randomUUID(), prefix + "new", 1)), "2999-01-03T00:00:00Z");
        registeredAt(adapter.save(Professional.register(UUID.randomUUID(), prefix + "mid", 1)), "2999-01-02T00:00:00Z");
        long rows = jdbc.queryForObject("SELECT count(*) FROM professional_management.professional", Long.class);

        Page<Professional> first = adapter.findPage(new PageRequest(1, 2));
        Page<Professional> second = adapter.findPage(new PageRequest(2, 1));

        assertThat(first.total()).isEqualTo(rows);
        assertThat(first.items()).extracting(Professional::getLicenseNumber)
                .containsExactly(prefix + "new", prefix + "mid");
        assertThat(second.items()).extracting(Professional::getLicenseNumber).containsExactly(prefix + "old");
        assertThat(adapter.findPage(new PageRequest(1000, 100)).items()).isEmpty();
    }

    @Test
    void profilesRegisteredAtTheSameInstantFollowTheirIdentifiers() {
        String prefix = "IT-TIE-" + UUID.randomUUID() + "-";
        for (String suffix : List.of("a", "b", "c")) {
            registeredAt(adapter.save(Professional.register(UUID.randomUUID(), prefix + suffix, 1)),
                    "2999-02-01T00:00:00Z");
        }

        List<Professional> page = adapter.findPage(new PageRequest(1, 3)).items();

        // PostgreSQL compares UUIDs byte by byte: the order of their lowercase text.
        assertThat(page).extracting(Professional::getLicenseNumber).allMatch(license -> license.startsWith(prefix));
        assertThat(page).extracting(professional -> professional.getId().toString())
                .isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    void theDatabaseRecordsWhenAProfileIsRegistered() {
        Professional saved = adapter.save(Professional.register(UUID.randomUUID(), license(), 1));

        assertThat(jdbc.queryForObject(
                "SELECT created_at = now() FROM professional_management.professional WHERE id = ?",
                Boolean.class, saved.getId())).isTrue();
    }

    @Test
    void aStatusChangeKeepsTheRegistrationTime() {
        Professional saved = adapter.save(Professional.register(UUID.randomUUID(), license(), 1));
        registeredAt(saved, "2020-05-06T07:08:09Z");

        adapter.save(saved.withStatus(ProfessionalStatus.INACTIVE));

        assertThat(jdbc.queryForObject("SELECT created_at = '2020-05-06T07:08:09Z'::timestamptz"
                + " FROM professional_management.professional WHERE id = ?", Boolean.class, saved.getId())).isTrue();
    }

    private void registeredAt(Professional professional, String instant) {
        jdbc.update("UPDATE professional_management.professional SET created_at = ?::timestamptz WHERE id = ?",
                instant, professional.getId());
    }

    @Test
    void theAccountAndLicenseLookupsSeeStoredProfessionals() {
        UUID account = UUID.randomUUID();
        String license = license();
        adapter.save(Professional.register(account, license, 1));

        assertThat(adapter.existsByIdentityUserId(account)).isTrue();
        assertThat(adapter.existsByIdentityUserId(UUID.randomUUID())).isFalse();
        assertThat(adapter.existsByLicenseNumber("  " + license + " ")).isTrue();
        assertThat(adapter.existsByLicenseNumber(license())).isFalse();
    }

    @Test
    void aSecondProfileForTheSameAccountIsADuplicateIdentity() {
        UUID account = UUID.randomUUID();
        adapter.save(Professional.register(account, license(), 1));

        assertThatThrownBy(() -> adapter.save(Professional.register(account, license(), 2)))
                .isInstanceOf(DuplicateProfessionalIdentityException.class);
    }

    @Test
    void aRepeatedLicenseIsADuplicateLicense() {
        String license = license();
        adapter.save(Professional.register(UUID.randomUUID(), license, 1));

        assertThatThrownBy(() -> adapter.save(Professional.register(UUID.randomUUID(), license, 2)))
                .isInstanceOf(DuplicateProfessionalLicenseException.class);
    }

    @Test
    void aStatusChangeUpdatesTheSameRow() {
        Professional saved = adapter.save(Professional.register(UUID.randomUUID(), license(), 2));
        long rows = jdbc.queryForObject("SELECT count(*) FROM professional_management.professional", Long.class);

        adapter.save(saved.withStatus(ProfessionalStatus.INACTIVE));

        assertThat(jdbc.queryForObject("SELECT status FROM professional_management.professional WHERE id = ?",
                String.class, saved.getId())).isEqualTo("INACTIVE");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM professional_management.professional", Long.class))
                .isEqualTo(rows);
    }

    @Test
    void everyConnectionCarriesTheStatementLimit() {
        // Numeral 5.3.10: hikari.connection-init-sql of application.yml.
        assertThat(jdbc.queryForObject("SHOW statement_timeout", String.class)).isEqualTo("5s");
    }

    @Test
    void theSchemaOnlyAcceptsGeneralPractitioners() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO professional_management.professional
                    (identity_user_id, license_number, professional_type)
                VALUES (?, ?, 'SPECIALIST')""", UUID.randomUUID(), license()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
