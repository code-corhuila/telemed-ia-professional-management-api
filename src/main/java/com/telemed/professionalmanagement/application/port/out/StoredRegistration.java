package com.telemed.professionalmanagement.application.port.out;

import com.telemed.professionalmanagement.domain.Professional;

/** A professional and the hash of the request that registered it under an idempotency key. */
public record StoredRegistration(Professional professional, String requestHash) {
}
