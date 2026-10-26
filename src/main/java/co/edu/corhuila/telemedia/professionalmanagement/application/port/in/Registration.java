package co.edu.corhuila.telemedia.professionalmanagement.application.port.in;

import co.edu.corhuila.telemedia.professionalmanagement.domain.model.Professional;

/**
 * The outcome of a registration: {@code created} is false when the same
 * {@code Idempotency-Key} had already registered this professional (Anexo C, numeral 5.3.8).
 */
public record Registration(Professional professional, boolean created) {
}
