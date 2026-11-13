package co.edu.corhuila.telemedia.professionalmanagement.application.port.in;

import java.util.Objects;
import java.util.Set;

/**
 * Who calls a use case, taken from a validated access token: its {@code sub} and roles.
 * Never from a header such as {@code X-User-Id}.
 */
public record Caller(String subject, Set<String> roles) {

    public static final String ADMIN = "ADMIN";
    /** Role of the tokens that identity issues to other services (`sub` = the client id). */
    public static final String SERVICE = "SERVICE";

    public Caller {
        Objects.requireNonNull(subject, "subject");
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /** A service token whose client id is one of {@code clients} (DEC-015). */
    public boolean isServiceClientOf(Set<String> clients) {
        return hasRole(SERVICE) && clients.contains(subject);
    }

    /** DEC-003: professional profiles and the catalog are administrative. */
    public void requireAdmin() {
        if (!hasRole(ADMIN)) {
            throw new AccessDeniedException();
        }
    }
}
