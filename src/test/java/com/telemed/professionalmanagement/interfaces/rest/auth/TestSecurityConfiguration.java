package com.telemed.professionalmanagement.interfaces.rest.auth;

import java.time.Clock;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** Web slice tests: the filter validates tokens signed with the test key pair. */
@TestConfiguration
public class TestSecurityConfiguration {

    @Bean
    Rs256Verifier rs256Verifier() {
        return new Rs256Verifier(TestTokens.trustedPublicKey(), Clock.systemUTC());
    }
}
