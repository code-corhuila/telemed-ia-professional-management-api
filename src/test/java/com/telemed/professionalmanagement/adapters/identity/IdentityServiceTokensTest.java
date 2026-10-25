package com.telemed.professionalmanagement.adapters.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemed.professionalmanagement.adapters.identity.IdentityServiceTokens.Settings;
import com.telemed.professionalmanagement.application.port.out.IdentityUnavailableException;

/** The service token of professional-management, from {@code POST /internal/service-token}. */
class IdentityServiceTokensTest {

    private static final String SECRET = "client-secret-for-tests-0001";
    private static final Instant T0 = Instant.parse("2026-10-24T20:00:00Z");

    private IdentityStub identity;
    private Instant now = T0;
    private final Clock clock = new Clock() {
        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    };

    @BeforeEach
    void start() throws Exception {
        identity = new IdentityStub();
        identity.tokenExpiry = () -> now.plus(Duration.ofMinutes(10));
    }

    @AfterEach
    void stop() {
        identity.close();
    }

    private IdentityServiceTokens tokens(URI url) {
        return new IdentityServiceTokens(HttpClient.newHttpClient(),
                new Settings(url, "professional-management", SECRET, Duration.ofMillis(500), Duration.ofMinutes(1)),
                clock);
    }

    @Test
    void asksForATokenWithTheClientCredentials() throws Exception {
        assertThat(tokens(identity.url()).token()).isEqualTo("token-1");

        var request = identity.to("/internal/service-token").get(0);
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.contentType()).startsWith("application/json");
        JsonNode body = new ObjectMapper().readTree(request.body());
        assertThat(body.get("clientId").asText()).isEqualTo("professional-management");
        assertThat(body.get("clientSecret").asText()).isEqualTo(SECRET);
    }

    @Test
    void keepsTheTokenUntilShortlyBeforeItExpires() {
        IdentityServiceTokens tokens = tokens(identity.url());
        tokens.token();
        now = T0.plus(Duration.ofMinutes(8));
        assertThat(tokens.token()).isEqualTo("token-1");

        now = T0.plus(Duration.ofMinutes(9));
        assertThat(tokens.token()).isEqualTo("token-2");
    }

    @Test
    void anInvalidatedTokenIsRequestedAgain() {
        IdentityServiceTokens tokens = tokens(identity.url());
        String first = tokens.token();

        tokens.invalidate(first);

        assertThat(tokens.token()).isEqualTo("token-2");
    }

    @Test
    void aRejectedClientMakesIdentityUnavailableWithoutTheSecret() {
        identity.tokenStatus = 401;

        assertThatThrownBy(() -> tokens(identity.url()).token())
                .isInstanceOf(IdentityUnavailableException.class)
                .hasMessageContaining("401")
                .hasMessageNotContaining(SECRET);
    }

    @Test
    void anUnreachableIdentityServiceIsUnavailable() {
        identity.close();

        assertThatThrownBy(() -> tokens(identity.url()).token()).isInstanceOf(IdentityUnavailableException.class);
    }

    @Test
    void theSettingsNeverPrintTheSecretAndRequireIt() {
        Settings settings = new Settings(URI.create("http://identity-api:8081"), "professional-management", SECRET,
                Duration.ofSeconds(5), Duration.ofMinutes(1));
        assertThat(settings.toString()).doesNotContain(SECRET);

        assertThatThrownBy(() -> new Settings(URI.create("http://identity-api:8081"), "professional-management", " ",
                Duration.ofSeconds(5), Duration.ofMinutes(1))).isInstanceOf(IllegalArgumentException.class);
    }
}
