package co.edu.corhuila.telemedia.professionalmanagement.adapter.out.identity;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.AccountDirectory;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.AccountName;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.IdentityUnavailableException;

/**
 * DEC-017: after {@code failureThreshold} consecutive failures of identity, the names are not
 * asked for {@code openFor}; the reads get {@link IdentityUnavailableException} at once and
 * return the professionals without names (DEC-008). When the time is over one call tries
 * identity again: a success closes the breaker, a failure opens it for the whole time again.
 * Only {@link IdentityUnavailableException} counts as a failure. The state is per instance.
 */
public final class CircuitBreakingAccountDirectory implements AccountDirectory {

    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakingAccountDirectory.class);

    private final AccountDirectory identity;
    private final int failureThreshold;
    private final Duration openFor;
    private final Clock clock;

    private int consecutiveFailures;
    private Instant openUntil;
    private boolean trialInFlight;

    public CircuitBreakingAccountDirectory(AccountDirectory identity, int failureThreshold, Duration openFor,
            Clock clock) {
        if (failureThreshold < 1) {
            throw new IllegalArgumentException("failureThreshold must be at least 1");
        }
        if (openFor == null || openFor.isNegative() || openFor.isZero()) {
            throw new IllegalArgumentException("openFor must be positive");
        }
        this.identity = Objects.requireNonNull(identity, "identity");
        this.failureThreshold = failureThreshold;
        this.openFor = openFor;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public Map<UUID, AccountName> namesOf(Collection<UUID> accountIds) {
        boolean trial = admit();
        Map<UUID, AccountName> names;
        try {
            names = identity.namesOf(accountIds);
        } catch (IdentityUnavailableException unavailable) {
            failed(trial);
            throw unavailable;
        } catch (RuntimeException other) {
            release(trial);
            throw other;
        }
        succeeded();
        return names;
    }

    /** Whether the call may go to identity, and if it is the trial call of an open breaker. */
    private synchronized boolean admit() {
        if (openUntil == null) {
            return false;
        }
        if (clock.instant().isBefore(openUntil) || trialInFlight) {
            throw new IdentityUnavailableException("identity names skipped while the circuit is open");
        }
        trialInFlight = true;
        return true;
    }

    private synchronized void failed(boolean trial) {
        if (trial) {
            trialInFlight = false;
            openUntil = clock.instant().plus(openFor);
            return;
        }
        consecutiveFailures++;
        if (openUntil == null && consecutiveFailures >= failureThreshold) {
            openUntil = clock.instant().plus(openFor);
            LOG.atWarn().addKeyValue("event", "professional.names.circuit_opened")
                    .addKeyValue("failures", consecutiveFailures)
                    .addKeyValue("open_for_s", openFor.toSeconds())
                    .log("account names skipped while identity keeps failing");
        }
    }

    private synchronized void release(boolean trial) {
        if (trial) {
            trialInFlight = false;
        }
    }

    private synchronized void succeeded() {
        boolean wasOpen = openUntil != null;
        consecutiveFailures = 0;
        openUntil = null;
        trialInFlight = false;
        if (wasOpen) {
            LOG.atInfo().addKeyValue("event", "professional.names.circuit_closed")
                    .log("account names read from identity again");
        }
    }
}
