package com.telemed.professionalmanagement.application.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** The identity domain, read through its API: the names of the professionals' accounts (DEC-008). */
@FunctionalInterface
public interface AccountDirectory {

    /**
     * The name of each account the identity domain knows; unknown accounts are left out.
     *
     * @throws IdentityUnavailableException when the identity service cannot answer now
     */
    Map<UUID, AccountName> namesOf(Collection<UUID> accountIds);
}
