package com.telemed.professionalmanagement.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemed.professionalmanagement.application.port.in.PageRequest;
import com.telemed.professionalmanagement.application.port.out.IdentityAccountConflictException;
import com.telemed.professionalmanagement.application.port.out.IdentityAccountPort;
import com.telemed.professionalmanagement.domain.Professional;
import com.telemed.professionalmanagement.interfaces.rest.auth.TestTokens;

/**
 * Contract checks of Anexo C, numeral 5.3.12, adapted to the professionals of this domain and
 * run by HTTP against the running service. As Anexo C sets for the HTTP level, the server reads
 * an in-memory repository ({@link InMemoryProfessionals}), so the checks run everywhere, CI
 * included. Every response of a contract route is also validated against the published
 * {@code 07-api/contracts/openapi/professional-service.yaml} (copied under
 * {@code src/test/resources/contract/}).
 *
 * <p>{@code POST /admin/register} creates the identity account too (DEC-003); here identity is
 * a double that answers as its contract will. Items of the checklist that do not apply: money
 * (no amounts), dates (the profile has none) and state transitions (none by HTTP).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProfessionalServiceContractTest {

    private static final String BASE = "/api/professionals";
    private static final String ROUTE = BASE + "/admin/register";
    private static final String ADMIN = TestTokens.bearer("admin-contract", "ADMIN");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final OpenApiInteractionValidator CONTRACT = OpenApiInteractionValidator
            .createForSpecificationUrl(ProfessionalServiceContractTest.class
                    .getResource("/contract/professional-service.yaml").toString())
            .withBasePathOverride(BASE)
            // The list schema is an allOf; merged first, so it is checked as one object.
            .withResolveCombinators(true)
            .build();

    private final String reader = UUID.randomUUID().toString();
    private final List<Professional> seeded = new ArrayList<>();

    @LocalServerPort
    private int port;

    @Autowired
    private InMemoryProfessionals professionals;

    @DynamicPropertySource
    static void environment(DynamicPropertyRegistry registry) {
        registry.add("JWT_PUBLIC_KEY", TestTokens::trustedPublicKeyPemOneLine);
    }

    @TestConfiguration
    static class InMemoryPersistence {

        @Bean
        @Primary
        InMemoryProfessionals inMemoryProfessionals() {
            return new InMemoryProfessionals();
        }

        /** Identity as its contract will answer: a new account id, or a conflict for a taken e-mail. */
        @Bean
        @Primary
        IdentityAccountPort identityDouble() {
            return (account, key) -> {
                if (account.email().startsWith("taken")) {
                    throw new IdentityAccountConflictException();
                }
                return UUID.randomUUID();
            };
        }
    }

    /** Three profiles; the contract has no creation route implemented yet (see the class note). */
    @BeforeAll
    void seedProfessionals() {
        for (String license : List.of("CT-0003", "CT-0001", "CT-0002")) {
            seeded.add(professionals.save(Professional.register(UUID.randomUUID(), license, 4)));
        }
    }

    // ---------------------------------------------------------------- Authentication

    @Nested
    class Authentication {

        @Test
        void healthAnswersWithoutAToken() throws Exception {
            assertThat(send(get("/health")).status()).isEqualTo(200);
        }

        @Test
        void withoutATokenEveryContractRouteIs401Unauthorized() throws Exception {
            assertError(send(get(BASE)), 401, "UNAUTHORIZED");
            assertError(send(get(BASE + "/" + seeded.get(0).getId())), 401, "UNAUTHORIZED");
        }

        @Test
        void aTokenWithoutAnRs256SignatureIs401() throws Exception {
            String payload = payload(reader, "PATIENT", 3600);
            for (String token : List.of(TestTokens.unsigned(payload), TestTokens.hs256WithPublicKeyAsSecret(payload),
                    TestTokens.signedWith(TestTokens.TRUSTED.getPrivate(), "{\"alg\":\"RS512\"}", payload))) {
                assertError(send(get(BASE).header("Authorization", "Bearer " + token)), 401, "UNAUTHORIZED");
            }
        }

        @Test
        void anExpiredTokenIs401() throws Exception {
            String token = TestTokens.rs256(payload(reader, "PATIENT", -3600));
            assertError(send(get(BASE).header("Authorization", "Bearer " + token)), 401, "UNAUTHORIZED");
        }

        @Test
        void aTokenSignedWithAnotherKeyIs401() throws Exception {
            String token = TestTokens.signedWith(TestTokens.OTHER.getPrivate(), "{\"alg\":\"RS256\"}",
                    payload(reader, "PATIENT", 3600));
            assertError(send(get(BASE).header("Authorization", "Bearer " + token)), 401, "UNAUTHORIZED");
        }

        @Test
        void the401SaysHowToAuthenticate() throws Exception {
            assertThat(send(get(BASE)).header("WWW-Authenticate")).isEqualTo("Bearer");
        }
    }

    // ------------------------------------------------------ Error envelope and correlation

    @Nested
    class EnvelopeAndCorrelation {

        @Test
        void theEnvelopeRepeatsTheReceivedCorrelationIdAsTraceIdAndInTheHeader() throws Exception {
            Reply reply = send(get(BASE).header("X-Correlation-Id", "contract-correlation-1"));

            assertThat(reply.json().get("traceId").asText()).isEqualTo("contract-correlation-1");
            assertThat(reply.header("X-Correlation-Id")).isEqualTo("contract-correlation-1");
        }

        @Test
        void withoutACorrelationIdTheServiceGeneratesOne() throws Exception {
            Reply reply = send(read(get(BASE)));

            assertThat(reply.header("X-Correlation-Id")).isNotBlank();
        }

        @Test
        void aMissingProfessionalIs404NotFound() throws Exception {
            assertError(send(read(get(BASE + "/" + UUID.randomUUID()))), 404, "NOT_FOUND");
        }

        @Test
        void anUnknownRouteIs404WithTheEnvelope() throws Exception {
            assertError(send(read(get("/api/unknown-route"))), 404, "NOT_FOUND");
        }

        @Test
        void theRemovedSpecialtyCatalogIs404() throws Exception {
            assertError(send(read(get("/api/specialties"))), 404, "NOT_FOUND");
        }

        @Test
        void anErrorNeverCarriesTheFrameworkFields() throws Exception {
            JsonNode error = send(read(get(BASE + "/" + UUID.randomUUID()))).json();

            assertThat(error.has("timestamp")).isFalse();
            assertThat(error.has("path")).isFalse();
            assertThat(error.has("trace")).isFalse();
        }
    }

    // ------------------------------------------------------------------ Representation

    @Nested
    class Representation {

        @Test
        void aProfessionalIsCamelCaseWithUuidsAndTheContractEnums() throws Exception {
            Professional stored = seeded.get(0);

            Reply reply = send(read(get(BASE + "/" + stored.getId())));

            assertThat(reply.status()).isEqualTo(200);
            JsonNode professional = reply.json();
            assertThat(professional.get("id").asText()).isEqualTo(stored.getId().toString());
            assertThat(professional.get("identityUserId").asText()).isEqualTo(stored.getIdentityUserId().toString());
            assertThat(professional.get("licenseNumber").asText()).isEqualTo(stored.getLicenseNumber());
            assertThat(professional.get("yearsExperience").isInt()).isTrue();
            assertThat(professional.get("professionalType").asText()).isEqualTo("GENERAL_PRACTITIONER");
            assertThat(professional.get("status").asText()).isEqualTo("ACTIVE");
            assertThat(professional.has("specialtyId")).isFalse();
        }
    }

    // ---------------------------------------------------------------------- Bounded list

    @Nested
    class BoundedList {

        @Test
        void theListIsDataAndMetaWithAtMostLimitItems() throws Exception {
            Reply reply = send(read(get(BASE + "?page=1&limit=2")));

            assertThat(reply.status()).isEqualTo(200);
            assertThat(reply.json().get("data").size()).isEqualTo(2);
            JsonNode meta = reply.json().get("meta");
            assertThat(meta.get("page").asInt()).isEqualTo(1);
            assertThat(meta.get("limit").asInt()).isEqualTo(2);
            // Other groups may have registered professionals in the same store.
            long total = storedTotal();
            assertThat(meta.get("total").asLong()).isEqualTo(total);
            assertThat(meta.get("totalPages").asLong()).isEqualTo((total + 1) / 2);
        }

        @Test
        void withoutLimitTwentyAreUsed() throws Exception {
            assertThat(send(read(get(BASE))).json().get("meta").get("limit").asInt()).isEqualTo(20);
        }

        @Test
        void pagesDoNotOverlapAndFollowTheLicenseOrder() throws Exception {
            List<String> licenses = new ArrayList<>();
            for (int page = 1; page <= 3; page++) {
                send(read(get(BASE + "?limit=1&page=" + page))).json().get("data")
                        .forEach(professional -> licenses.add(professional.get("licenseNumber").asText()));
            }

            assertThat(licenses).containsExactly("CT-0001", "CT-0002", "CT-0003");
        }

        @Test
        void aPagePastTheEndIsEmptyButKeepsTheTotal() throws Exception {
            Reply reply = send(read(get(BASE + "?page=1000&limit=2")));

            assertThat(reply.json().get("data").size()).isZero();
            assertThat(reply.json().get("meta").get("total").asLong()).isEqualTo(storedTotal());
        }

        @Test
        void aPageOrLimitOutOfRangeIs400() throws Exception {
            for (String query : List.of("?page=0", "?limit=0", "?limit=101", "?page=abc")) {
                assertError(send(read(get(BASE + query))), 400, "VALIDATION_ERROR");
            }
        }

        @Test
        void aMalformedIdIs400ValidationError() throws Exception {
            assertError(send(read(get(BASE + "/not-a-uuid"))), 400, "VALIDATION_ERROR");
        }
    }

    // ----------------------------------------------------------- Idempotent registration

    @Nested
    class IdempotentRegistration {

        @Test
        void theFirstRequestIs201WithAUuidAndItsLocation() throws Exception {
            Reply reply = send(register("contract-" + UUID.randomUUID(), body("CT-REG-1", "ana1@telemed.test")));

            assertThat(reply.status()).isEqualTo(201);
            String id = reply.json().get("id").asText();
            assertThat(UUID.fromString(id).toString()).isEqualTo(id);
            assertThat(reply.header("Location")).isEqualTo("/api/v1/professionals/" + id);
            assertThat(reply.json().get("status").asText()).isEqualTo("ACTIVE");
        }

        @Test
        void aRetryWithTheSameKeyIs200WithTheSameId() throws Exception {
            String key = "contract-" + UUID.randomUUID();
            Reply first = send(register(key, body("CT-REG-2", "ana2@telemed.test")));

            Reply retry = send(register(key, body("CT-REG-2", "ana2@telemed.test")));

            assertThat(retry.status()).isEqualTo(200);
            assertThat(retry.json().get("id").asText()).isEqualTo(first.json().get("id").asText());
        }

        @Test
        void theSameKeyWithAnotherBodyIs422() throws Exception {
            String key = "contract-" + UUID.randomUUID();
            send(register(key, body("CT-REG-3", "ana3@telemed.test")));

            assertError(send(register(key, body("CT-REG-4", "ana4@telemed.test"))), 422, "BUSINESS_RULE_VIOLATION");
        }

        @Test
        void aTakenAccountOrLicenseIs409() throws Exception {
            assertError(send(register("contract-" + UUID.randomUUID(), body("CT-REG-5", "taken@telemed.test"))),
                    409, "CONFLICT");
            send(register("contract-" + UUID.randomUUID(), body("CT-REG-6", "ana6@telemed.test")));
            assertError(send(register("contract-" + UUID.randomUUID(), body("CT-REG-6", "ana7@telemed.test"))),
                    409, "CONFLICT");
        }

        @Test
        void anInvalidBodyNamesEveryInvalidFieldIncludingTheIdempotencyKeyHeader() throws Exception {
            Reply reply = send(post(ROUTE, "{\"email\":\"nope\"}").header("Authorization", ADMIN));

            assertError(reply, 400, "VALIDATION_ERROR");
            assertThat(reply.json().get("details").findValuesAsText("field"))
                    .contains("Idempotency-Key", "fullName", "email", "password", "licenseNumber");
        }

        @Test
        void aPatientCannotRegister() throws Exception {
            assertError(send(read(post(ROUTE, body("CT-REG-8", "ana8@telemed.test"))
                    .header("Idempotency-Key", "contract-" + UUID.randomUUID()))), 403, "FORBIDDEN");
        }

        private HttpRequest.Builder register(String key, String body) {
            return post(ROUTE, body).header("Authorization", ADMIN).header("Idempotency-Key", key);
        }

        private static String body(String license, String email) {
            return "{\"fullName\":\"Ana Gómez\",\"email\":\"" + email + "\",\"identityDocument\":\"" + email
                    + "\",\"password\":\"temporary-pass-1\",\"licenseNumber\":\"" + license
                    + "\",\"yearsExperience\":5}";
        }
    }

    // ------------------------------------------------------------------ Profile status

    @Nested
    class ProfileStatus {

        private Reply change(String id, String status) throws Exception {
            return send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + BASE + "/" + id + "/status"))
                    .header("Content-Type", "application/json").header("Authorization", ADMIN)
                    .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"status\":\"" + status + "\"}")));
        }

        @Test
        void anAdministratorDeactivatesAndReactivatesAProfile() throws Exception {
            String id = professionals.save(Professional.register(UUID.randomUUID(), "CT-ST-1", 2)).getId().toString();

            Reply inactive = change(id, "INACTIVE");
            Reply again = change(id, "INACTIVE");
            Reply active = change(id, "ACTIVE");

            assertThat(inactive.status()).isEqualTo(200);
            assertThat(inactive.json().get("status").asText()).isEqualTo("INACTIVE");
            assertThat(again.status()).isEqualTo(200);
            assertThat(active.json().get("status").asText()).isEqualTo("ACTIVE");
            assertThat(active.json().get("id").asText()).isEqualTo(id);
        }

        @Test
        void aStatusOutsideTheContractIs400() throws Exception {
            assertError(change(seeded.get(0).getId().toString(), "SUSPENDED"), 400, "VALIDATION_ERROR");
        }

        @Test
        void anUnknownProfessionalIs404() throws Exception {
            assertError(change(UUID.randomUUID().toString(), "ACTIVE"), 404, "NOT_FOUND");
        }

        @Test
        void aPatientCannotChangeTheStatus() throws Exception {
            Reply reply = send(read(HttpRequest.newBuilder(URI.create(
                            "http://localhost:" + port + BASE + "/" + seeded.get(0).getId() + "/status"))
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"status\":\"INACTIVE\"}"))));

            assertError(reply, 403, "FORBIDDEN");
        }
    }

    // ------------------------------------------------------------------------- helpers

    private long storedTotal() {
        return professionals.findPage(new PageRequest(1, PageRequest.MAX_LIMIT)).total();
    }

    private record Reply(int status, Map<String, List<String>> headers, String body, JsonNode json) {

        String header(String name) {
            return headers.entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase(name))
                    .map(entry -> entry.getValue().get(0)).findFirst().orElse(null);
        }
    }

    private static String payload(String subject, String role, long secondsFromNow) {
        long exp = java.time.Instant.now().getEpochSecond() + secondsFromNow;
        return "{\"sub\":\"" + subject + "\",\"role\":\"" + role + "\",\"exp\":" + exp + "}";
    }

    private HttpRequest.Builder get(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
    }

    /** Any authenticated caller reads professionals; a patient is the least privileged one. */
    private HttpRequest.Builder post(String path, String body) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
    }

    private HttpRequest.Builder read(HttpRequest.Builder request) {
        return request.header("Authorization", TestTokens.bearer(reader, "PATIENT"));
    }

    /**
     * Sends the request and, for a contract route, checks the response against the published
     * contract: status code, body schema and headers of the operation.
     */
    private static Reply send(HttpRequest.Builder builder) throws IOException, InterruptedException {
        HttpRequest request = builder.build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode json = response.body().isBlank() ? JSON.nullNode() : JSON.readTree(response.body());
        String path = request.uri().getPath();
        if (path.startsWith(BASE)) {
            SimpleResponse.Builder contractResponse = SimpleResponse.Builder.status(response.statusCode())
                    .withBody(response.body());
            response.headers().map().forEach((name, values) -> contractResponse.withHeader(name, values));
            ValidationReport report = CONTRACT.validateResponse(path.equals(BASE) ? BASE + "/" : path,
                    Request.Method.valueOf(request.method()), contractResponse.build());
            assertThat(report.getMessages()).as("%s %s against professional-service.yaml", request.method(), path)
                    .isEmpty();
        }
        return new Reply(response.statusCode(), response.headers().map(), response.body(), json);
    }

    private static void assertError(Reply reply, int status, String code) {
        assertThat(reply.status()).as(reply.body()).isEqualTo(status);
        assertThat(reply.json().get("error").asText()).isEqualTo(code);
        assertThat(reply.json().get("message").asText()).isNotBlank();
        assertThat(reply.json().get("traceId").asText()).isNotBlank();
    }
}
