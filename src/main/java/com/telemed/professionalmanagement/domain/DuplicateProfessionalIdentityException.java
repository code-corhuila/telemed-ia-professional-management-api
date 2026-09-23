package com.telemed.professionalmanagement.domain;

public class DuplicateProfessionalIdentityException extends DomainException {

    public DuplicateProfessionalIdentityException(Long identityUserId) {
        super("A professional already exists for identityUserId: " + identityUserId);
    }
}
