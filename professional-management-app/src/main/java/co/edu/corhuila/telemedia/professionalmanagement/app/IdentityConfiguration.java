package co.edu.corhuila.telemedia.professionalmanagement.app;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.edu.corhuila.telemedia.professionalmanagement.adapter.out.identity.HttpAccountDirectory;
import co.edu.corhuila.telemedia.professionalmanagement.adapter.out.identity.HttpIdentityAccounts;
import co.edu.corhuila.telemedia.professionalmanagement.adapter.out.identity.IdentityServiceTokens;
import co.edu.corhuila.telemedia.professionalmanagement.adapter.out.identity.UnavailableIdentityAccounts;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.AccountDirectory;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.IdentityAccountPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.java11.instrument.binder.jdk.MicrometerHttpClient;
import io.micrometer.observation.ObservationRegistry;

/**
 * The identity service that creates the professionals' accounts (DEC-003) and returns their
 * names (DEC-008). Without the client secret the service still starts: registrations answer
 * 503 and the professionals are read without names.
 */
@Configuration
public class IdentityConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(IdentityConfiguration.class);

    /** One client and one token cache for both identity adapters. */
    private record Identity(HttpClient http, IdentityServiceTokens tokens, URI url) { }

    private final MeterRegistry meters;
    private final ObservationRegistry observations;

    private Identity identity;

    /** Without instrumentation: the calls to identity are neither measured nor traced. */
    public IdentityConfiguration() {
        this(new SimpleMeterRegistry(), ObservationRegistry.NOOP);
    }

    /** The calls to identity are measured and carry the current trace (NFR-005). */
    @Autowired
    public IdentityConfiguration(MeterRegistry meters, ObservationRegistry observations) {
        this.meters = meters;
        this.observations = observations;
    }

    @Bean
    IdentityAccountPort identityAccounts(
            @Value("${identity.url}") String identityUrl,
            @Value("${identity.client-id}") String clientId,
            @Value("${identity.client-secret:}") String clientSecret,
            @Value("${identity.request-timeout}") Duration requestTimeout,
            @Value("${identity.token-refresh-margin}") Duration refreshMargin) {
        if (clientSecret == null || clientSecret.isBlank()) {
            LOG.warn("SERVICE_CLIENT_SECRET is not set: professional registrations answer 503");
            return new UnavailableIdentityAccounts();
        }
        Identity client = identity(identityUrl, clientId, clientSecret, requestTimeout, refreshMargin);
        return new HttpIdentityAccounts(client.http(), client.tokens(), client.url(), requestTimeout);
    }

    @Bean
    AccountDirectory accountDirectory(
            @Value("${identity.url}") String identityUrl,
            @Value("${identity.client-id}") String clientId,
            @Value("${identity.client-secret:}") String clientSecret,
            @Value("${identity.request-timeout}") Duration requestTimeout,
            @Value("${identity.token-refresh-margin}") Duration refreshMargin) {
        if (clientSecret == null || clientSecret.isBlank()) {
            return accountIds -> Map.of();
        }
        Identity client = identity(identityUrl, clientId, clientSecret, requestTimeout, refreshMargin);
        return new HttpAccountDirectory(client.http(), client.tokens(), client.url(), requestTimeout);
    }

    private synchronized Identity identity(String identityUrl, String clientId, String clientSecret,
            Duration requestTimeout, Duration refreshMargin) {
        if (identity == null) {
            URI url = URI.create(identityUrl);
            HttpClient http = MicrometerHttpClient.instrumentationBuilder(
                    HttpClient.newBuilder().connectTimeout(requestTimeout).build(), meters)
                    .observationRegistry(observations).build();
            identity = new Identity(http, new IdentityServiceTokens(http,
                    new IdentityServiceTokens.Settings(url, clientId, clientSecret, requestTimeout, refreshMargin),
                    Clock.systemUTC()), url);
        }
        return identity;
    }
}
