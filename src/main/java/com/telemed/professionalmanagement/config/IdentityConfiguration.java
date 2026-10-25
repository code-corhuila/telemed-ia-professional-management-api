package com.telemed.professionalmanagement.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.telemed.professionalmanagement.adapters.identity.HttpIdentityAccounts;
import com.telemed.professionalmanagement.adapters.identity.IdentityServiceTokens;
import com.telemed.professionalmanagement.adapters.identity.UnavailableIdentityAccounts;
import com.telemed.professionalmanagement.application.port.out.IdentityAccountPort;

/**
 * The identity service that creates the professionals' accounts (DEC-003). Without the client
 * secret the service still starts, and registrations answer 503 until it is configured.
 */
@Configuration
public class IdentityConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(IdentityConfiguration.class);

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
        URI identity = URI.create(identityUrl);
        HttpClient http = HttpClient.newBuilder().connectTimeout(requestTimeout).build();
        IdentityServiceTokens tokens = new IdentityServiceTokens(http,
                new IdentityServiceTokens.Settings(identity, clientId, clientSecret, requestTimeout, refreshMargin),
                Clock.systemUTC());
        return new HttpIdentityAccounts(http, tokens, identity, requestTimeout);
    }
}
