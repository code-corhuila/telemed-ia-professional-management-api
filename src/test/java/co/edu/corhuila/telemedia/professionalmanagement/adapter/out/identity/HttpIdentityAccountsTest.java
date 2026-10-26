package co.edu.corhuila.telemedia.professionalmanagement.adapter.out.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.edu.corhuila.telemedia.professionalmanagement.adapter.out.identity.IdentityServiceTokens.Settings;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.IdempotencyKeyReusedException;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.IdentityAccountConflictException;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.IdentityAccountRejectedException;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.IdentityUnavailableException;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.NewProfessionalAccount;

/**
 * {@code POST /internal/accounts} of the identity service (identity-and-access-api #122):
 * service token of a client in its allowed list, {@code Idempotency-Key}, role
 * {@code PROFESSIONAL}; {@code 201 {id}} or {@code 200} on a repeated key,
 * {@code 409 REGISTRATION_CONFLICT}, {@code 422 IDEMPOTENCY_KEY_REUSED},
 * {@code 400 INVALID_REGISTRATION}, {@code 401} and {@code 403}.
 */
class HttpIdentityAccountsTest {

    private static final String KEY = "register-0001";
    private static final String SECRET = "client-secret-for-tests-0001";
    private static final UUID ACCOUNT = UUID.fromString("d1000000-0000-4000-8000-000000000001");
    private static final NewProfessionalAccount NEW = new NewProfessionalAccount(
            "Ana Gómez", "ana@telemed.test", "1075000001", "temporary-pass-1");

    private IdentityStub identity;
    private HttpIdentityAccounts accounts;

    @BeforeEach
    void start() throws Exception {
        identity = new IdentityStub();
        HttpClient http = HttpClient.newHttpClient();
        IdentityServiceTokens tokens = new IdentityServiceTokens(http, new Settings(identity.url(),
                "professional-management", SECRET, Duration.ofMillis(500), Duration.ofMinutes(1)),
                Clock.systemUTC());
        accounts = new HttpIdentityAccounts(http, tokens, identity.url(), Duration.ofMillis(500));
    }

    @AfterEach
    void stop() {
        identity.close();
        MDC.clear();
    }

    @Test
    void createsTheProfessionalAccountWithTheServiceTokenAndTheSameKey() throws Exception {
        identity.answerAccounts(201, "{\"id\":\"" + ACCOUNT + "\"}");
        MDC.put("correlationId", "trace-accounts");

        assertThat(accounts.createProfessionalAccount(NEW, KEY)).isEqualTo(ACCOUNT);

        var request = identity.to("/internal/accounts").get(0);
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.authorization()).isEqualTo("Bearer token-1");
        assertThat(request.idempotencyKey()).isEqualTo(KEY);
        assertThat(request.correlationId()).isEqualTo("trace-accounts");
        assertThat(request.contentType()).startsWith("application/json");
        JsonNode body = new ObjectMapper().readTree(request.body());
        assertThat(body.get("fullName").asText()).isEqualTo("Ana Gómez");
        assertThat(body.get("email").asText()).isEqualTo("ana@telemed.test");
        assertThat(body.get("identityDocument").asText()).isEqualTo("1075000001");
        assertThat(body.get("password").asText()).isEqualTo("temporary-pass-1");
        assertThat(body.get("role").asText()).isEqualTo("PROFESSIONAL");
        assertThat(body.size()).isEqualTo(5);
    }

    @Test
    void aRepeatedKeyAnsweredWith200GivesTheSameAccount() {
        identity.answerAccounts(200, "{\"id\":\"" + ACCOUNT + "\"}");

        assertThat(accounts.createProfessionalAccount(NEW, KEY)).isEqualTo(ACCOUNT);
    }

    @Test
    void theTokenIsReusedBetweenAccounts() {
        identity.answerAccounts(201, "{\"id\":\"" + ACCOUNT + "\"}");
        identity.answerAccounts(201, "{\"id\":\"" + UUID.randomUUID() + "\"}");

        accounts.createProfessionalAccount(NEW, KEY);
        accounts.createProfessionalAccount(NEW, "register-0002");

        assertThat(identity.tokensIssued).hasValue(1);
    }

    @Test
    void anEmailOrDocumentAlreadyRegisteredIsAConflict() {
        identity.answerAccounts(409, "{\"error\":\"REGISTRATION_CONFLICT\",\"message\":\"x\",\"traceId\":\"t\"}");

        assertThatThrownBy(() -> accounts.createProfessionalAccount(NEW, KEY))
                .isInstanceOf(IdentityAccountConflictException.class);
    }

    @Test
    void theSameKeyWithOtherDataIsAReusedKey() {
        identity.answerAccounts(422, "{\"error\":\"IDEMPOTENCY_KEY_REUSED\",\"message\":\"x\",\"traceId\":\"t\"}");

        assertThatThrownBy(() -> accounts.createProfessionalAccount(NEW, KEY))
                .isInstanceOf(IdempotencyKeyReusedException.class);
    }

    @Test
    void dataTheIdentityServiceRejectsIsRejectedWithoutThePassword() {
        identity.answerAccounts(400, "{\"error\":\"INVALID_REGISTRATION\",\"message\":\"x\",\"traceId\":\"t\"}");

        assertThatThrownBy(() -> accounts.createProfessionalAccount(NEW, KEY))
                .isInstanceOf(IdentityAccountRejectedException.class)
                .hasMessageNotContaining("temporary-pass-1");
    }

    @Test
    void aRejectedTokenIsRenewedOnceAndTheCallRepeatedWithTheSameKey() {
        identity.answerAccounts(401, "{\"error\":\"UNAUTHORIZED\"}");
        identity.answerAccounts(201, "{\"id\":\"" + ACCOUNT + "\"}");

        assertThat(accounts.createProfessionalAccount(NEW, KEY)).isEqualTo(ACCOUNT);

        var calls = identity.to("/internal/accounts");
        assertThat(calls).hasSize(2);
        assertThat(calls.get(1).authorization()).isEqualTo("Bearer token-2");
        assertThat(calls.get(1).idempotencyKey()).isEqualTo(KEY);
    }

    @Test
    void aTokenRejectedTwiceMakesIdentityUnavailable() {
        identity.answerAccounts(401, "{}");
        identity.answerAccounts(401, "{}");

        assertThatThrownBy(() -> accounts.createProfessionalAccount(NEW, KEY))
                .isInstanceOf(IdentityUnavailableException.class);
        assertThat(identity.to("/internal/accounts")).hasSize(2);
    }

    @Test
    void aClientOutsideTheAllowedListMakesIdentityUnavailable() {
        identity.answerAccounts(403, "{\"error\":\"FORBIDDEN\"}");

        assertThatThrownBy(() -> accounts.createProfessionalAccount(NEW, KEY))
                .isInstanceOf(IdentityUnavailableException.class)
                .hasMessageContaining("403");
    }

    @Test
    void aServerErrorOrAnUnreadableAnswerMakesIdentityUnavailable() {
        identity.answerAccounts(503, "{}");
        identity.answerAccounts(201, "{\"id\":\"not-a-uuid\"}");
        identity.answerAccounts(201, "");

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> accounts.createProfessionalAccount(NEW, KEY))
                    .isInstanceOf(IdentityUnavailableException.class)
                    .hasMessageNotContaining("temporary-pass-1")
                    .hasMessageNotContaining(SECRET);
        }
    }

    @Test
    void anUnreachableIdentityServiceIsUnavailable() {
        identity.close();

        assertThatThrownBy(() -> accounts.createProfessionalAccount(NEW, KEY))
                .isInstanceOf(IdentityUnavailableException.class);
    }
}
