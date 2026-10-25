package com.telemed.professionalmanagement.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.telemed.professionalmanagement.adapters.identity.HttpAccountDirectory;
import com.telemed.professionalmanagement.adapters.identity.HttpIdentityAccounts;
import com.telemed.professionalmanagement.adapters.identity.IdentityServiceTokens;
import com.telemed.professionalmanagement.adapters.identity.UnavailableIdentityAccounts;
import com.telemed.professionalmanagement.application.port.out.AccountDirectory;
import com.telemed.professionalmanagement.application.port.out.IdentityAccountPort;

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

    private Identity identity;

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
            HttpClient http = HttpClient.newBuilder().connectTimeout(requestTimeout).build();
            identity = new Identity(http, new IdentityServiceTokens(http,
                    new IdentityServiceTokens.Settings(url, clientId, clientSecret, requestTimeout, refreshMargin),
                    Clock.systemUTC()), url);
        }
        return identity;
    }
}
