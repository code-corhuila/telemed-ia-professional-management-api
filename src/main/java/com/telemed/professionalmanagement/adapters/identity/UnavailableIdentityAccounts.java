package com.telemed.professionalmanagement.adapters.identity;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telemed.professionalmanagement.application.port.out.IdentityAccountPort;
import com.telemed.professionalmanagement.application.port.out.IdentityUnavailableException;
import com.telemed.professionalmanagement.application.port.out.NewProfessionalAccount;

/**
 * Used when this service has no client secret for the identity service: every registration
 * fails closed here, before anything is stored, so there is never a profile without its
 * account. The rest of the API works as usual.
 */
public class UnavailableIdentityAccounts implements IdentityAccountPort {

    private static final Logger LOG = LoggerFactory.getLogger(UnavailableIdentityAccounts.class);

    @Override
    public UUID createProfessionalAccount(NewProfessionalAccount account, String idempotencyKey) {
        LOG.warn("professional registration refused: SERVICE_CLIENT_SECRET is not configured");
        throw new IdentityUnavailableException("no client secret for the identity service");
    }
}
