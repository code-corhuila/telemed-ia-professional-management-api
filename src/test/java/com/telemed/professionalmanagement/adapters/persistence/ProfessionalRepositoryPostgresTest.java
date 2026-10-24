package com.telemed.professionalmanagement.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void aPageIsOrderedByLicenseAndCountsTheWholeTable() {
        String prefix = "IT-PAGE-" + UUID.randomUUID() + "-";
        adapter.save(Professional.register(UUID.randomUUID(), prefix + "c", 1));
        adapter.save(Professional.register(UUID.randomUUID(), prefix + "a", 1));
        adapter.save(Professional.register(UUID.randomUUID(), prefix + "b", 1));
        long rows = jdbc.queryForObject("SELECT count(*) FROM professional_management.professional", Long.class);

        Page<Professional> first = adapter.findPage(new PageRequest(1, 2));
        Page<Professional> all = adapter.findPage(new PageRequest(1, 100));

        assertThat(first.total()).isEqualTo(rows);
        assertThat(first.items()).hasSize(2);
        assertThat(all.items()).extracting(Professional::getLicenseNumber).isSorted()
                .containsSubsequence(prefix + "a", prefix + "b", prefix + "c");
        assertThat(adapter.findPage(new PageRequest(1000, 100)).items()).isEmpty();
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
    void theSchemaOnlyAcceptsGeneralPractitioners() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO professional_management.professional
                    (identity_user_id, license_number, professional_type)
                VALUES (?, ?, 'SPECIALIST')""", UUID.randomUUID(), license()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
