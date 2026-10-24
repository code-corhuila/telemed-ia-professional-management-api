package com.telemed.professionalmanagement.application.port.in;

/** The {@code Idempotency-Key} already registered a different request. */
public class IdempotencyKeyReusedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public IdempotencyKeyReusedException() {
        super("the Idempotency-Key was already used for a different registration");
    }
}
