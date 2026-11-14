package co.edu.corhuila.telemedia.professionalmanagement.adapter.out.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.AccountDirectory;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.AccountName;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.IdentityUnavailableException;

/** DEC-017: the names are skipped while identity keeps failing, and tried again after a while. */
class CircuitBreakingAccountDirectoryTest {

    private static final UUID ACCOUNT = UUID.fromString("c1000000-0000-4000-8000-000000000001");
    private static final List<UUID> IDS = List.of(ACCOUNT);
    private static final Map<UUID, AccountName> NAMES = Map.of(ACCOUNT, new AccountName("Ana Gómez", null));
    private static final Duration OPEN_FOR = Duration.ofSeconds(30);

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-14T15:00:00Z"));
    private final FakeDirectory identity = new FakeDirectory();
    private final CircuitBreakingAccountDirectory directory =
            new CircuitBreakingAccountDirectory(identity, 3, OPEN_FOR, clock);

    private void failTimes(int times) {
        for (int i = 0; i < times; i++) {
            assertThatThrownBy(() -> directory.namesOf(IDS)).isInstanceOf(IdentityUnavailableException.class);
        }
    }

    @Test
    void whileIdentityAnswersEveryCallReachesIt() {
        assertThat(directory.namesOf(IDS)).isEqualTo(NAMES);
        assertThat(directory.namesOf(IDS)).isEqualTo(NAMES);

        assertThat(identity.calls).isEqualTo(2);
    }

    @Test
    void fewerFailuresThanTheThresholdKeepCallingIdentity() {
        identity.down = true;
        failTimes(2);
        identity.down = false;

        assertThat(directory.namesOf(IDS)).isEqualTo(NAMES);
        assertThat(identity.calls).isEqualTo(3);
    }

    @Test
    void aSuccessResetsTheCountOfFailures() {
        identity.down = true;
        failTimes(2);
        identity.down = false;
        directory.namesOf(IDS);
        identity.down = true;
        failTimes(2);

        identity.down = false;
        assertThat(directory.namesOf(IDS)).isEqualTo(NAMES);
    }

    @Test
    void afterTheThresholdTheCallsFailAtOnceWithoutReachingIdentity() {
        identity.down = true;
        failTimes(3);
        identity.down = false;

        assertThatThrownBy(() -> directory.namesOf(IDS)).isInstanceOf(IdentityUnavailableException.class);
        clock.advance(OPEN_FOR.minusSeconds(1));
        assertThatThrownBy(() -> directory.namesOf(IDS)).isInstanceOf(IdentityUnavailableException.class);

        assertThat(identity.calls).isEqualTo(3);
    }

    @Test
    void whenTheTimeIsOverOneCallTriesIdentityAndASuccessClosesTheBreaker() {
        identity.down = true;
        failTimes(3);
        identity.down = false;
        clock.advance(OPEN_FOR);

        assertThat(directory.namesOf(IDS)).isEqualTo(NAMES);
        assertThat(directory.namesOf(IDS)).isEqualTo(NAMES);
        assertThat(identity.calls).isEqualTo(5);
    }

    @Test
    void aFailedTrialOpensTheBreakerAgainForTheWholeTime() {
        identity.down = true;
        failTimes(3);
        clock.advance(OPEN_FOR);

        failTimes(1);
        assertThat(identity.calls).isEqualTo(4);

        identity.down = false;
        clock.advance(OPEN_FOR.minusSeconds(1));
        assertThatThrownBy(() -> directory.namesOf(IDS)).isInstanceOf(IdentityUnavailableException.class);
        assertThat(identity.calls).isEqualTo(4);

        clock.advance(Duration.ofSeconds(1));
        assertThat(directory.namesOf(IDS)).isEqualTo(NAMES);
    }

    @Test
    void otherErrorsAreNotCountedAsIdentityFailures() {
        identity.failure = new IllegalStateException("bug");
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> directory.namesOf(IDS)).isInstanceOf(IllegalStateException.class);
        }

        assertThat(identity.calls).isEqualTo(4);
    }

    @Test
    void theSettingsMustMakeSense() {
        assertThatThrownBy(() -> new CircuitBreakingAccountDirectory(identity, 0, OPEN_FOR, clock))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CircuitBreakingAccountDirectory(identity, 3, Duration.ZERO, clock))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static final class FakeDirectory implements AccountDirectory {

        private boolean down;
        private RuntimeException failure;
        private int calls;

        @Override
        public Map<UUID, AccountName> namesOf(Collection<UUID> accountIds) {
            calls++;
            if (failure != null) {
                throw failure;
            }
            if (down) {
                throw new IdentityUnavailableException("identity could not be reached");
            }
            return NAMES;
        }
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
