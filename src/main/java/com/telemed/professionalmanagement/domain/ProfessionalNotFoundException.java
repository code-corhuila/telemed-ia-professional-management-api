package com.telemed.professionalmanagement.domain;

public class ProfessionalNotFoundException extends DomainException {

    public ProfessionalNotFoundException(Long professionalId) {
        super("Professional not found with id: " + professionalId);
    }
}
