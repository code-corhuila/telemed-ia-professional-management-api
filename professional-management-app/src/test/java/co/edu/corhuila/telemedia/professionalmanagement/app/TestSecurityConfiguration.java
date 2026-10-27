package co.edu.corhuila.telemedia.professionalmanagement.app;

import java.time.Clock;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import co.edu.corhuila.telemedia.professionalmanagement.adapter.in.http.Rs256Verifier;
import co.edu.corhuila.telemedia.professionalmanagement.adapter.in.http.TestTokens;

/** Web slice tests: the filter validates tokens signed with the test key pair. */
@TestConfiguration
public class TestSecurityConfiguration {

    @Bean
    Rs256Verifier rs256Verifier() {
        return new Rs256Verifier(TestTokens.trustedPublicKey(), Clock.systemUTC());
    }
}
