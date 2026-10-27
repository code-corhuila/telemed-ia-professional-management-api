package co.edu.corhuila.telemedia.professionalmanagement.adapter.out.identity;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.MDC;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.IdempotencyKeyReusedException;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.IdentityAccountConflictException;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.IdentityAccountPort;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.IdentityAccountRejectedException;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.IdentityUnavailableException;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.NewProfessionalAccount;

/**
 * Creates the professional's account at {@code POST /internal/accounts} of the identity
 * service (identity-and-access-api #122, DEC-003), with a service token of this client and
 * the registration's {@code Idempotency-Key}, so a repeated call never creates a second
 * account. A token identity rejects is renewed once and the call repeated with the same key.
 *
 * <p>The password travels only in the request body: it is never logged nor put in a message.
 */
public final class HttpIdentityAccounts implements IdentityAccountPort {

    static final String PATH = "/internal/accounts";
    /** Set by the correlation filter for the current request. */
    private static final String CORRELATION_MDC_KEY = "correlationId";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient http;
    private final IdentityServiceTokens tokens;
    private final URI endpoint;
    private final Duration requestTimeout;

    public HttpIdentityAccounts(HttpClient http, IdentityServiceTokens tokens, URI identityUrl,
            Duration requestTimeout) {
        this.http = Objects.requireNonNull(http, "http");
        this.tokens = Objects.requireNonNull(tokens, "tokens");
        this.endpoint = resolve(identityUrl, PATH);
        this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout");
    }

    static URI resolve(URI base, String path) {
        String url = base.toString();
        return URI.create((url.endsWith("/") ? url.substring(0, url.length() - 1) : url) + path);
    }

    @Override
    public UUID createProfessionalAccount(NewProfessionalAccount account, String idempotencyKey) {
        String body = body(account);
        String token = tokens.token();
        HttpResponse<byte[]> response = send(body, idempotencyKey, token);
        if (response.statusCode() == 401) {
            tokens.invalidate(token);
            response = send(body, idempotencyKey, tokens.token());
        }
        return switch (response.statusCode()) {
            case 200, 201 -> accountId(response.body());
            case 400 -> throw new IdentityAccountRejectedException();
            case 409 -> throw new IdentityAccountConflictException();
            case 422 -> throw new IdempotencyKeyReusedException();
            default -> throw new IdentityUnavailableException("the account creation answered " + response.statusCode());
        };
    }

    private HttpResponse<byte[]> send(String body, String idempotencyKey, String token) {
        HttpRequest.Builder request = HttpRequest.newBuilder(endpoint)
                .timeout(requestTimeout)
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", idempotencyKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        String correlationId = MDC.get(CORRELATION_MDC_KEY);
        if (correlationId != null) {
            request.header("X-Correlation-Id", correlationId);
        }
        try {
            return http.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new IdentityUnavailableException("the identity service could not be reached");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IdentityUnavailableException("interrupted while creating the account");
        }
    }

    private static String body(NewProfessionalAccount account) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("fullName", account.fullName());
        fields.put("email", account.email());
        fields.put("identityDocument", account.identityDocument());
        fields.put("password", account.password());
        fields.put("role", "PROFESSIONAL");
        try {
            return JSON.writeValueAsString(fields);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("the account request could not be written");
        }
    }

    private static UUID accountId(byte[] body) {
        try {
            JsonNode root = JSON.readTree(body);
            JsonNode id = root == null ? null : root.get("id");
            if (id == null || !id.isTextual()) {
                throw invalid();
            }
            return UUID.fromString(id.asText());
        } catch (IOException | IllegalArgumentException e) {
            throw invalid();
        }
    }

    private static IdentityUnavailableException invalid() {
        return new IdentityUnavailableException("invalid account creation answer");
    }
}
