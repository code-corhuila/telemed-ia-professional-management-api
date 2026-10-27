package co.edu.corhuila.telemedia.professionalmanagement.application.port.out;

import co.edu.corhuila.telemedia.professionalmanagement.domain.model.Professional;

/** A professional and the hash of the request that registered it under an idempotency key. */
public record StoredRegistration(Professional professional, String requestHash) {
}
