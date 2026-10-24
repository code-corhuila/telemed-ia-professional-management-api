package com.telemed.professionalmanagement.application.port.out;

import java.util.Optional;

import com.telemed.professionalmanagement.domain.Professional;

public interface ProfessionalRegistrationStore {

    Optional<StoredRegistration> findByIdempotencyKey(String idempotencyKey);

    boolean existsByLicenseNumber(String licenseNumber);

    /**
     * Stores the profile and its idempotency key in one transaction (Anexo C, numeral 5.3.8).
     * Empty when the key was already taken: then nothing is stored.
     */
    Optional<Professional> saveRegistration(Professional professional, String idempotencyKey, String requestHash);
}
