package com.telemed.professionalmanagement.application.port.out;

/** The identity domain already has an account with that e-mail or document. */
public class IdentityAccountConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public IdentityAccountConflictException() {
        super("an account with that e-mail or identity document already exists");
    }
}
