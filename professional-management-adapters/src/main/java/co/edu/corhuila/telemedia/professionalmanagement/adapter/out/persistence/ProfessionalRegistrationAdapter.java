package co.edu.corhuila.telemedia.professionalmanagement.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.ProfessionalRegistrationStore;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.ProfessionalRepositoryPort;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.StoredRegistration;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.Professional;

/**
 * Registration with its idempotency key (Anexo C, numeral 5.3.8), on the tables of
 * {@code telemed-ia-professional-management-db}: in one transaction the profile is inserted
 * and then the key, with {@code ON CONFLICT DO NOTHING}. If the key already existed, the
 * transaction is rolled back and the new profile disappears.
 */
@Component
public class ProfessionalRegistrationAdapter implements ProfessionalRegistrationStore {

    private static final String INSERT_KEY = """
            INSERT INTO professional_management.idempotency_key (key, request_hash, professional_id)
            VALUES (?, ?, ?)
            ON CONFLICT (key) DO NOTHING""";

    private static final String FIND_KEY = """
            SELECT request_hash, professional_id
            FROM professional_management.idempotency_key
            WHERE key = ?""";

    private final ProfessionalRepositoryPort professionals;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public ProfessionalRegistrationAdapter(ProfessionalRepositoryPort professionals, JdbcTemplate jdbc,
                                           PlatformTransactionManager transactionManager) {
        this.professionals = professionals;
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Override
    public Optional<StoredRegistration> findByIdempotencyKey(String idempotencyKey) {
        return jdbc.query(FIND_KEY, (row, index) -> new KeyRow(row.getString("request_hash"),
                        row.getObject("professional_id", UUID.class)), idempotencyKey)
                .stream()
                .findFirst()
                .flatMap(key -> professionals.findById(key.professionalId())
                        .map(professional -> new StoredRegistration(professional, key.requestHash())));
    }

    @Override
    public boolean existsByLicenseNumber(String licenseNumber) {
        return professionals.existsByLicenseNumber(licenseNumber);
    }

    @Override
    public Optional<Professional> saveRegistration(Professional professional, String idempotencyKey,
                                                   String requestHash) {
        return transaction.execute(status -> {
            // A repeated license or account fails here and rolls everything back.
            Professional saved = professionals.save(professional);
            if (jdbc.update(INSERT_KEY, idempotencyKey, requestHash, saved.getId()) == 0) {
                status.setRollbackOnly();
                return Optional.empty();
            }
            return Optional.of(saved);
        });
    }

    private record KeyRow(String requestHash, UUID professionalId) {
    }
}
