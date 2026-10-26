package co.edu.corhuila.telemedia.professionalmanagement.adapter.out.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import co.edu.corhuila.telemedia.professionalmanagement.adapter.out.identity.IdentityServiceTokens.Settings;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.AccountName;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.IdentityUnavailableException;

/**
 * {@code GET /internal/accounts?ids=...} of the identity service (identity-and-access-api #128):
 * service token of {@code professional-management}, at most 100 ids, {@code {data: [{id,
 * fullName, email}]}} with only the professionals' accounts, {@code 400} for an empty or
 * malformed list (DEC-008).
 */
class HttpAccountDirectoryTest {

    private static final UUID ANA = UUID.fromString("d1000000-0000-4000-8000-000000000001");
    private static final UUID LUIS = UUID.fromString("d1000000-0000-4000-8000-000000000002");
    private static final UUID UNKNOWN = UUID.fromString("d1000000-0000-4000-8000-000000000003");

    private IdentityStub identity;
    private HttpAccountDirectory directory;

    @BeforeEach
    void start() throws Exception {
        identity = new IdentityStub();
        HttpClient http = HttpClient.newHttpClient();
        IdentityServiceTokens tokens = new IdentityServiceTokens(http, new Settings(identity.url(),
                "professional-management", "client-secret-for-tests-0001", Duration.ofMillis(500),
                Duration.ofMinutes(1)), Clock.systemUTC());
        directory = new HttpAccountDirectory(http, tokens, identity.url(), Duration.ofMillis(500));
    }

    @AfterEach
    void stop() {
        identity.close();
        MDC.clear();
    }

    private static String account(UUID id, String fullName, String email) {
        return "{\"id\":\"" + id + "\",\"fullName\":\"" + fullName + "\",\"email\":\"" + email + "\"}";
    }

    private static List<String> ids(IdentityStub.Request request) {
        String query = URLDecoder.decode(request.query(), StandardCharsets.UTF_8);
        assertThat(query).startsWith("ids=");
        return List.of(query.substring("ids=".length()).split(","));
    }

    @Test
    void readsTheNamesOfThePageInOneCallWithTheServiceToken() {
        identity.answerAccounts(200, "{\"data\":[" + account(ANA, "Ana Gómez", "ana@telemed.test") + ","
                + account(LUIS, "Luis Ruiz", "luis@telemed.test") + "]}");
        MDC.put("correlationId", "trace-names");

        var names = directory.namesOf(List.of(ANA, LUIS, UNKNOWN));

        assertThat(names).containsOnlyKeys(ANA, LUIS);
        assertThat(names.get(ANA)).isEqualTo(new AccountName("Ana Gómez", "ana@telemed.test"));
        var request = identity.to("/internal/accounts").get(0);
        assertThat(request.method()).isEqualTo("GET");
        assertThat(request.authorization()).isEqualTo("Bearer token-1");
        assertThat(request.correlationId()).isEqualTo("trace-names");
        assertThat(ids(request)).containsExactlyInAnyOrder(ANA.toString(), LUIS.toString(), UNKNOWN.toString());
    }

    @Test
    void noAccountsNeedNoCall() {
        assertThat(directory.namesOf(List.of())).isEmpty();
        assertThat(identity.requests).isEmpty();
    }

    @Test
    void repeatedAccountsAreAskedOnce() {
        identity.answerAccounts(200, "{\"data\":[]}");

        directory.namesOf(List.of(ANA, ANA, LUIS));

        assertThat(ids(identity.to("/internal/accounts").get(0))).hasSize(2);
    }

    @Test
    void moreThan100AccountsAreAskedInGroupsOf100() {
        identity.answerAccounts(200, "{\"data\":[]}");
        identity.answerAccounts(200, "{\"data\":[]}");
        List<UUID> many = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            many.add(UUID.randomUUID());
        }

        directory.namesOf(many);

        var calls = identity.to("/internal/accounts");
        assertThat(calls).hasSize(2);
        assertThat(ids(calls.get(0))).hasSize(100);
        assertThat(ids(calls.get(1))).hasSize(50);
    }

    @Test
    void aRejectedTokenIsRenewedOnce() {
        identity.answerAccounts(401, "{}");
        identity.answerAccounts(200, "{\"data\":[" + account(ANA, "Ana Gómez", "ana@telemed.test") + "]}");

        assertThat(directory.namesOf(List.of(ANA))).containsOnlyKeys(ANA);
        assertThat(identity.to("/internal/accounts").get(1).authorization()).isEqualTo("Bearer token-2");
    }

    @Test
    void anyOtherAnswerMakesIdentityUnavailable() {
        identity.answerAccounts(400, "{\"error\":\"VALIDATION_ERROR\"}");
        identity.answerAccounts(403, "{}");
        identity.answerAccounts(500, "{}");
        identity.answerAccounts(200, "{\"data\":[{\"id\":\"not-a-uuid\",\"fullName\":\"x\",\"email\":\"y\"}]}");
        identity.answerAccounts(200, "");

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> directory.namesOf(List.of(ANA)))
                    .isInstanceOf(IdentityUnavailableException.class);
        }
    }

    @Test
    void anUnreachableIdentityServiceIsUnavailable() {
        identity.close();

        assertThatThrownBy(() -> directory.namesOf(List.of(ANA))).isInstanceOf(IdentityUnavailableException.class);
    }
}
