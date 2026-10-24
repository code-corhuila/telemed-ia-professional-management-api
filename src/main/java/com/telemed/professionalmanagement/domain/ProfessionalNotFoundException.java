package com.telemed.professionalmanagement.domain;

import java.util.UUID;

public class ProfessionalNotFoundException extends DomainException {

    public ProfessionalNotFoundException(UUID professionalId) {
        super("Professional not found with id: " + professionalId);
    }
}
