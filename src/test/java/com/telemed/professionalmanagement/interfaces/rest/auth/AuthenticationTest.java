package com.telemed.professionalmanagement.interfaces.rest.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Numeral 5.3.7 on the running service: every route but {@code /health} validates the RS256
 * token itself, receiving the public key through {@code JWT_PUBLIC_KEY}; only an ADMIN
 * registers professionals (DEC-003).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthenticationTest {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON = new ParameterizedTypeReference<>() { };
    private static final String VALID_PROFESSIONAL =
            "{\"identityUserId\":\"c1000000-0000-4000-8000-000000000099\",\"licenseNumber\":\"AUTH-123\",\"yearsExperience\":3}";

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void jwt(DynamicPropertyRegistry registry) {
        registry.add("JWT_PUBLIC_KEY", TestTokens::trustedPublicKeyPemOneLine);
    }

    private ResponseEntity<Map<String, Object>> list(String authorization) {
        HttpHeaders headers = new HttpHeaders();
        if (authorization != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authorization);
        }
        return restTemplate.exchange("/api/professionals", HttpMethod.GET, new HttpEntity<>(headers), JSON);
    }

    private ResponseEntity<Map<String, Object>> register(String authorization) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange("/api/professionals", HttpMethod.POST,
                new HttpEntity<>(VALID_PROFESSIONAL, headers), JSON);
    }

    private static String payload(String role, long exp) {
        return "{\"sub\":\"user-1\",\"role\":\"" + role + "\",\"exp\":" + exp + "}";
    }

    private static long inAnHour() {
        return Instant.now().getEpochSecond() + 3600;
    }

    private static void assertUnauthorized(ResponseEntity<Map<String, Object>> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("error", "UNAUTHORIZED").containsKey("traceId");
        assertThat(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
    }

    @Test
    void withoutATokenItIs401WithTheEnvelope() {
        assertUnauthorized(list(null));
    }

    @Test
    void aTokenWithAnotherAlgorithmIs401() {
        assertUnauthorized(list("Bearer " + TestTokens.unsigned(payload("ADMIN", inAnHour()))));
        assertUnauthorized(list("Bearer " + TestTokens.hs256WithPublicKeyAsSecret(payload("ADMIN", inAnHour()))));
    }

    @Test
    void anExpiredTokenIs401() {
        assertUnauthorized(list("Bearer " + TestTokens.rs256(payload("ADMIN", Instant.now().getEpochSecond() - 3600))));
    }

    @Test
    void aTokenSignedWithAnotherKeyIs401() {
        assertUnauthorized(list("Bearer " + TestTokens.signedWith(TestTokens.OTHER.getPrivate(),
                "{\"alg\":\"RS256\",\"typ\":\"JWT\"}", payload("ADMIN", inAnHour()))));
    }

    @Test
    void anyAuthenticatedUserReadsTheProfessionals() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, TestTokens.bearer("patient-1", "PATIENT"));

        // The list is still a bare array; the paginated {data, meta} comes with the contract routes.
        assertThat(restTemplate.exchange("/api/professionals", HttpMethod.GET, new HttpEntity<>(headers), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void onlyAnAdministratorRegistersAProfessional() {
        ResponseEntity<Map<String, Object>> patient = register(TestTokens.bearer("patient-1", "PATIENT"));

        assertThat(patient.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(patient.getBody()).containsEntry("error", "FORBIDDEN").containsKey("traceId");
        assertThat(register(TestTokens.bearer("admin-1", "ADMIN")).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
