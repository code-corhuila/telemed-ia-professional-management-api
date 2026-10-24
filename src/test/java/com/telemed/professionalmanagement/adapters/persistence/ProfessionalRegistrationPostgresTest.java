package com.telemed.professionalmanagement.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.telemed.professionalmanagement.adapters.persistence.adapter.ProfessionalRegistrationAdapter;
import com.telemed.professionalmanagement.adapters.persistence.mapper.ProfessionalPersistenceMapper;
import com.telemed.professionalmanagement.application.port.out.StoredRegistration;
import com.telemed.professionalmanagement.domain.DuplicateProfessionalLicenseException;
import com.telemed.professionalmanagement.domain.Professional;

/**
 * The registration store against the real engine and the schema of
 * {@code telemed-ia-professional-management-db}: the profile and its idempotency key go in
 * one transaction (Anexo C, numeral 5.3.8). The tests commit for real, so the rollback of a
 * repeated key can be observed; each test deletes what it created. Without
 * {@code TEST_DATABASE_URL} the class is skipped (Anexo I).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ProfessionalRegistrationAdapter.class, ProfessionalPersistenceMapper.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@EnabledIfEnvironmentVariable(named = "TEST_DATABASE_URL", matches = ".+")
class ProfessionalRegistrationPostgresTest {

    @DynamicPropertySource
    static void realDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("TEST_DATABASE_URL"));
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private ProfessionalRegistrationAdapter store;

    @Autowired
    private JdbcTemplate jdbc;

    private final List<String> licenses = new ArrayList<>();

    @AfterEach
    void deleteWhatTheTestCreated() {
        // The idempotency key goes with its professional (ON DELETE CASCADE).
        licenses.forEach(license -> jdbc.update(
                "DELETE FROM professional_management.professional WHERE license_number = ?", license));
    }

    private Professional newProfessional() {
        String license = "IT-REG-" + UUID.randomUUID();
        licenses.add(license);
        return Professional.register(UUID.randomUUID(), license, 4);
    }

    private static String key() {
        return "it-key-" + UUID.randomUUID();
    }

    private int professionalsWithLicense(String license) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM professional_management.professional WHERE license_number = ?",
                Integer.class, license);
    }

    @Test
    void theProfileAndItsKeyAreStoredTogether() {
        String key = key();

        Professional saved = store.saveRegistration(newProfessional(), key, "hash-a").orElseThrow();

        assertThat(jdbc.queryForMap(
                "SELECT professional_id, request_hash FROM professional_management.idempotency_key WHERE key = ?",
                key))
                .containsEntry("professional_id", saved.getId())
                .containsEntry("request_hash", "hash-a");
        StoredRegistration stored = store.findByIdempotencyKey(key).orElseThrow();
        assertThat(stored.professional().getId()).isEqualTo(saved.getId());
        assertThat(stored.requestHash()).isEqualTo("hash-a");
        assertThat(store.existsByLicenseNumber(saved.getLicenseNumber())).isTrue();
    }

    @Test
    void aRepeatedKeyRollsTheNewProfileBack() {
        String key = key();
        store.saveRegistration(newProfessional(), key, "hash-a").orElseThrow();
        Professional second = newProfessional();

        assertThat(store.saveRegistration(second, key, "hash-a")).isEmpty();
        assertThat(professionalsWithLicense(second.getLicenseNumber())).isZero();
    }

    @Test
    void anUnknownKeyIsEmpty() {
        assertThat(store.findByIdempotencyKey(key())).isEmpty();
    }

    @Test
    void aRepeatedLicenseIsADuplicateAndStoresNoKey() {
        Professional first = newProfessional();
        store.saveRegistration(first, key(), "hash-a").orElseThrow();
        String key = key();

        assertThatThrownBy(() -> store.saveRegistration(
                Professional.register(UUID.randomUUID(), first.getLicenseNumber(), 1), key, "hash-b"))
                .isInstanceOf(DuplicateProfessionalLicenseException.class);
        assertThat(store.findByIdempotencyKey(key)).isEmpty();
    }
}
