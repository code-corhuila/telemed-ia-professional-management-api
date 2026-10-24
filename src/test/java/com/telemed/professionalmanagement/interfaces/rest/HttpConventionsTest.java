package com.telemed.professionalmanagement.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * Anexo C conventions on the running service: the shared error envelope
 * {@code {error, message, details?, traceId}}, {@code X-Correlation-Id} (numeral 5.3.9),
 * one JSON log line per request, and {@code GET /health} without a token.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class HttpConventionsTest {

    @org.springframework.test.context.DynamicPropertySource
    static void jwt(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("JWT_PUBLIC_KEY",
                com.telemed.professionalmanagement.interfaces.rest.auth.TestTokens::trustedPublicKeyPemOneLine);
    }

    private static final ParameterizedTypeReference<Map<String, Object>> JSON = new ParameterizedTypeReference<>() { };

    @Autowired
    private TestRestTemplate restTemplate;

    private ResponseEntity<Map<String, Object>> get(String path, HttpHeaders headers) {
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), JSON);
    }

    private static HttpHeaders correlated(String correlationId) {
        HttpHeaders headers = authorized();
        headers.set("X-Correlation-Id", correlationId);
        return headers;
    }

    private static HttpHeaders authorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION,
                com.telemed.professionalmanagement.interfaces.rest.auth.TestTokens.bearer("admin-1", "ADMIN"));
        return headers;
    }

    @Test
    void healthAnswersWithoutAToken() {
        ResponseEntity<Map<String, Object>> response = get("/health", new HttpHeaders());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "UP");
    }

    @Test
    void anUnknownRouteAnswers404WithTheEnvelopeAndTheReceivedTrace() {
        ResponseEntity<Map<String, Object>> response = get("/api/no-such-route", correlated("trace-pm-0001"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "NOT_FOUND").containsKey("message")
                .containsEntry("traceId", "trace-pm-0001").doesNotContainKeys("timestamp", "status", "path");
        assertThat(response.getHeaders().getFirst("X-Correlation-Id")).isEqualTo("trace-pm-0001");
    }

    @Test
    void withoutTheHeaderTheServiceGeneratesOne() {
        ResponseEntity<Map<String, Object>> response = get("/api/no-such-route", authorized());

        String generated = response.getHeaders().getFirst("X-Correlation-Id");
        assertThat(generated).isNotBlank();
        assertThat(response.getBody()).containsEntry("traceId", generated);
    }

    @Test
    void anUnsafeCorrelationIdIsReplacedSoItCannotInjectIntoTheLogs() {
        ResponseEntity<Map<String, Object>> response = get("/health", correlated("bad\"value{}"));

        assertThat(response.getHeaders().getFirst("X-Correlation-Id")).isNotEqualTo("bad\"value{}")
                .matches("[A-Za-z0-9._:-]{1,128}");
    }

    @Test
    void malformedJsonIs400ValidationError() {
        HttpHeaders headers = correlated("trace-pm-0002");
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange("/api/professionals", HttpMethod.POST,
                new HttpEntity<>("{bad json", headers), JSON);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "VALIDATION_ERROR")
                .containsEntry("traceId", "trace-pm-0002");
    }

    @Test
    void aMethodTheRouteDoesNotAcceptIs405WithTheEnvelope() {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange("/health", HttpMethod.DELETE,
                new HttpEntity<>(correlated("trace-pm-0003")), JSON);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).containsEntry("error", "METHOD_NOT_ALLOWED")
                .containsEntry("traceId", "trace-pm-0003");
    }

    @Test
    void everyRequestWritesAJsonLogLineWithItsCorrelationId(CapturedOutput output) {
        get("/health", correlated("trace-pm-0004"));

        assertThat(output.getOut().lines().filter(line -> line.contains("GET /health")))
                .anySatisfy(line -> assertThat(line).startsWith("{").contains("\"correlationId\":\"trace-pm-0004\""));
    }
}
