package co.edu.corhuila.telemedia.professionalmanagement.adapter.out.identity;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.MDC;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.AccountDirectory;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.AccountName;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.IdentityUnavailableException;

/**
 * The names of the professionals' accounts at {@code GET /internal/accounts?ids=...} of the
 * identity service (identity-and-access-api #128, DEC-008), with the service token of this
 * client. Identity accepts at most 100 ids per call and leaves out the accounts it does not
 * return; a token it rejects is renewed once.
 */
public final class HttpAccountDirectory implements AccountDirectory {

    static final int MAX_IDS = 100;
    /** Set by the correlation filter for the current request. */
    private static final String CORRELATION_MDC_KEY = "correlationId";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient http;
    private final IdentityServiceTokens tokens;
    private final URI endpoint;
    private final Duration requestTimeout;

    public HttpAccountDirectory(HttpClient http, IdentityServiceTokens tokens, URI identityUrl,
            Duration requestTimeout) {
        this.http = Objects.requireNonNull(http, "http");
        this.tokens = Objects.requireNonNull(tokens, "tokens");
        this.endpoint = HttpIdentityAccounts.resolve(identityUrl, HttpIdentityAccounts.PATH);
        this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout");
    }

    @Override
    public Map<UUID, AccountName> namesOf(Collection<UUID> accountIds) {
        List<UUID> distinct = accountIds.stream().distinct().toList();
        Map<UUID, AccountName> names = new HashMap<>();
        for (int from = 0; from < distinct.size(); from += MAX_IDS) {
            names.putAll(read(distinct.subList(from, Math.min(from + MAX_IDS, distinct.size()))));
        }
        return names;
    }

    private Map<UUID, AccountName> read(List<UUID> ids) {
        URI uri = URI.create(endpoint + "?ids=" + URLEncoder.encode(
                ids.stream().map(UUID::toString).collect(Collectors.joining(",")), StandardCharsets.UTF_8));
        String token = tokens.token();
        HttpResponse<byte[]> response = send(uri, token);
        if (response.statusCode() == 401) {
            tokens.invalidate(token);
            response = send(uri, tokens.token());
        }
        if (response.statusCode() != 200) {
            throw new IdentityUnavailableException("the account names answered " + response.statusCode());
        }
        return names(response.body());
    }

    private HttpResponse<byte[]> send(URI uri, String token) {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET();
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
            throw new IdentityUnavailableException("interrupted while reading the account names");
        }
    }

    private static Map<UUID, AccountName> names(byte[] body) {
        try {
            JsonNode root = JSON.readTree(body);
            JsonNode data = root == null ? null : root.get("data");
            if (data == null || !data.isArray()) {
                throw invalid();
            }
            Map<UUID, AccountName> names = new HashMap<>();
            for (JsonNode account : data) {
                JsonNode id = account.get("id");
                JsonNode fullName = account.get("fullName");
                JsonNode email = account.get("email");
                if (id == null || !id.isTextual() || fullName == null || !fullName.isTextual()
                        || email == null || !email.isTextual()) {
                    throw invalid();
                }
                names.put(UUID.fromString(id.asText()), new AccountName(fullName.asText(), email.asText()));
            }
            return names;
        } catch (IOException | IllegalArgumentException e) {
            throw invalid();
        }
    }

    private static IdentityUnavailableException invalid() {
        return new IdentityUnavailableException("invalid account names answer");
    }
}
