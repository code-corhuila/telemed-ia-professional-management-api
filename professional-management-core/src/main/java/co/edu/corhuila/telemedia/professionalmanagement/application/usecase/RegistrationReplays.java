package co.edu.corhuila.telemedia.professionalmanagement.application.usecase;

import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.IdempotencyKeyReusedException;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.Registration;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.ProfessionalRegistrationStore;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.StoredRegistration;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.DomainException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * The idempotency rules shared by every operation that creates a profile under an
 * {@code Idempotency-Key} (Anexo C, numeral 5.3.8): the same key and the same request return
 * the first result; the same key with another request is rejected.
 */
final class RegistrationReplays {

    private RegistrationReplays() {
    }

    static Registration replay(StoredRegistration previous, String requestHash) {
        if (!previous.requestHash().equals(requestHash)) {
            throw new IdempotencyKeyReusedException();
        }
        return new Registration(previous.professional(), false);
    }

    /**
     * A concurrent twin with the same key may have stored the profile after the key was
     * checked: then the duplicate is that same request, answered as a retry.
     */
    static Registration replayOrThrow(ProfessionalRegistrationStore store, String idempotencyKey,
                                      String requestHash, DomainException duplicate) {
        return store.findByIdempotencyKey(idempotencyKey)
                .map(twin -> replay(twin, requestHash))
                .orElseThrow(() -> duplicate);
    }

    static String sha256(String canonical) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required by every Java runtime", impossible);
        }
    }
}
