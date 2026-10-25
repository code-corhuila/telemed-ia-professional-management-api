package com.telemed.professionalmanagement.application.port.out;

/**
 * The identity domain refused the account data (for example a password outside its policy);
 * nothing was created.
 */
public class IdentityAccountRejectedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public IdentityAccountRejectedException() {
        super("the identity service rejected the account data");
    }
}
