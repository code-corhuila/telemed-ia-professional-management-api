package com.telemed.professionalmanagement.adapters.identity;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.telemed.professionalmanagement.application.port.out.IdentityAccountPort;
import com.telemed.professionalmanagement.application.port.out.IdentityUnavailableException;
import com.telemed.professionalmanagement.application.port.out.NewProfessionalAccount;

/**
 * The identity service does not expose account creation to other services yet. Until it
 * does, every registration fails closed here, before anything is stored: no account and no
 * profile without its account. The HTTP adapter replaces this class when the identity
 * contract defines that operation.
 */
@Component
public class UnavailableIdentityAccounts implements IdentityAccountPort {

    private static final Logger LOG = LoggerFactory.getLogger(UnavailableIdentityAccounts.class);

    @Override
    public UUID createProfessionalAccount(NewProfessionalAccount account, String idempotencyKey) {
        LOG.warn("professional registration refused: the identity service does not create accounts yet");
        throw new IdentityUnavailableException("the identity service does not create professional accounts yet");
    }
}
