package com.telemed.professionalmanagement.contract;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.telemed.professionalmanagement.application.port.in.Page;
import com.telemed.professionalmanagement.application.port.in.PageRequest;
import com.telemed.professionalmanagement.application.port.out.ProfessionalRegistrationStore;
import com.telemed.professionalmanagement.application.port.out.ProfessionalRepositoryPort;
import com.telemed.professionalmanagement.application.port.out.StoredRegistration;
import com.telemed.professionalmanagement.domain.DuplicateProfessionalIdentityException;
import com.telemed.professionalmanagement.domain.DuplicateProfessionalLicenseException;
import com.telemed.professionalmanagement.domain.Professional;

/**
 * The repository port in memory, for the HTTP-level contract checks (Anexo C). The SQL is
 * covered by {@code ProfessionalRepositoryPostgresTest}. Same order as the adapter: by license.
 */
class InMemoryProfessionals implements ProfessionalRepositoryPort, ProfessionalRegistrationStore {

    private final Map<UUID, Professional> rows = new ConcurrentHashMap<>();
    private final Map<String, StoredRegistration> keys = new ConcurrentHashMap<>();

    @Override
    public synchronized Professional save(Professional professional) {
        if (professional.getId() != null && rows.containsKey(professional.getId())) {
            // An existing profile is updated in place, as the JPA adapter merges it.
            rows.put(professional.getId(), professional);
            return professional;
        }
        if (existsByIdentityUserId(professional.getIdentityUserId())) {
            throw new DuplicateProfessionalIdentityException(professional.getIdentityUserId());
        }
        if (existsByLicenseNumber(professional.getLicenseNumber())) {
            throw new DuplicateProfessionalLicenseException(professional.getLicenseNumber());
        }
        Professional stored = new Professional(UUID.randomUUID(), professional.getIdentityUserId(),
                professional.getLicenseNumber(), professional.getYearsExperience(),
                professional.getProfessionalType(), professional.getStatus());
        rows.put(stored.getId(), stored);
        return stored;
    }

    @Override
    public Optional<Professional> findById(UUID professionalId) {
        return Optional.ofNullable(rows.get(professionalId));
    }

    @Override
    public Page<Professional> findPage(PageRequest page) {
        List<Professional> ordered = rows.values().stream()
                .sorted(Comparator.comparing(Professional::getLicenseNumber))
                .toList();
        List<Professional> items = ordered.stream().skip(page.offset()).limit(page.limit()).toList();
        return Page.of(items, page, ordered.size());
    }

    @Override
    public boolean existsByIdentityUserId(UUID identityUserId) {
        return rows.values().stream().anyMatch(row -> row.getIdentityUserId().equals(identityUserId));
    }

    @Override
    public boolean existsByLicenseNumber(String licenseNumber) {
        return licenseNumber != null
                && rows.values().stream().anyMatch(row -> row.getLicenseNumber().equals(licenseNumber.trim()));
    }

    @Override
    public Optional<StoredRegistration> findByIdempotencyKey(String idempotencyKey) {
        return Optional.ofNullable(keys.get(idempotencyKey));
    }

    @Override
    public synchronized Optional<Professional> saveRegistration(Professional professional, String idempotencyKey,
                                                                String requestHash) {
        if (keys.containsKey(idempotencyKey)) {
            return Optional.empty();
        }
        Professional saved = save(professional);
        keys.put(idempotencyKey, new StoredRegistration(saved, requestHash));
        return Optional.of(saved);
    }
}
