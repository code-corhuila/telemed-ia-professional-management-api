package com.telemed.professionalmanagement.domain;

public class SpecialtyNotFoundException extends DomainException {

    public SpecialtyNotFoundException(Long specialtyId) {
        super("Specialty not found with id: " + specialtyId);
    }
}
