package co.edu.corhuila.telemedia.professionalmanagement.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import co.edu.corhuila.telemedia.professionalmanagement.adapter.out.identity.HttpAccountDirectory;
import co.edu.corhuila.telemedia.professionalmanagement.adapter.out.identity.HttpIdentityAccounts;
import co.edu.corhuila.telemedia.professionalmanagement.adapter.out.identity.UnavailableIdentityAccounts;

/** Which identity adapter the composition root wires. */
class IdentityConfigurationTest {

    private final IdentityConfiguration configuration = new IdentityConfiguration();

    @Test
    void withTheClientSecretRegistrationsGoToTheIdentityService() {
        assertThat(configuration.identityAccounts("http://identity-api:8081", "professional-management",
                "client-secret-for-tests-0001", Duration.ofSeconds(5), Duration.ofMinutes(1)))
                .isInstanceOf(HttpIdentityAccounts.class);
    }

    @Test
    void withoutTheClientSecretRegistrationsFailClosed() {
        assertThat(configuration.identityAccounts("http://identity-api:8081", "professional-management", "",
                Duration.ofSeconds(5), Duration.ofMinutes(1)))
                .isInstanceOf(UnavailableIdentityAccounts.class);
    }

    @Test
    void withTheClientSecretTheNamesComeFromTheIdentityService() {
        assertThat(configuration.accountDirectory("http://identity-api:8081", "professional-management",
                "client-secret-for-tests-0001", Duration.ofSeconds(5), Duration.ofMinutes(1)))
                .isInstanceOf(HttpAccountDirectory.class);
    }

    @Test
    void withoutTheClientSecretThereAreNoNames() {
        assertThat(configuration.accountDirectory("http://identity-api:8081", "professional-management", "",
                Duration.ofSeconds(5), Duration.ofMinutes(1)).namesOf(List.of(UUID.randomUUID()))).isEmpty();
    }
}
