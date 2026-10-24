package com.telemed.professionalmanagement.domain;

import java.util.UUID;

public class DuplicateProfessionalIdentityException extends DomainException {

    public DuplicateProfessionalIdentityException(UUID identityUserId) {
        super("A professional already exists for identityUserId: " + identityUserId);
    }
}
